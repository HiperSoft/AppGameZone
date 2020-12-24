package com.hunabsys.gamezone.views.activities.prizes

/* ktlint-disable no-wildcard-imports */
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AbsListView
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.*
import com.hunabsys.gamezone.models.daos.EvidenceDao
import com.hunabsys.gamezone.models.daos.GameMachineDao
import com.hunabsys.gamezone.models.daos.PrizeDao
import com.hunabsys.gamezone.models.daos.RouteDao
import com.hunabsys.gamezone.models.datamodels.Evidence
import com.hunabsys.gamezone.models.datamodels.GameMachine
import com.hunabsys.gamezone.models.datamodels.Prize
import com.hunabsys.gamezone.services.delegates.IEvidenceDelegate
import com.hunabsys.gamezone.services.delegates.IPrizeDelegate
import com.hunabsys.gamezone.services.rest.HttpClientService
import com.hunabsys.gamezone.services.rest.SynchronizeEvidencesService
import com.hunabsys.gamezone.services.rest.SynchronizePrizesService
import com.hunabsys.gamezone.storage.storage.StorageAccess
import com.hunabsys.gamezone.views.activities.BaseActivity
import com.hunabsys.gamezone.views.activities.ScannerActivity
import com.hunabsys.gamezone.views.activities.prizes.IPrizeConstants.Companion.EVIDENCE_TYPE
import com.hunabsys.gamezone.views.adapters.PrizesListAdapter
import com.rollbar.android.Rollbar
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.android.synthetic.main.activity_prizes.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class PrizesActivity : BaseActivity(), IPrizeDelegate, IEvidenceDelegate {

    private val tag = PrizesActivity::class.java.simpleName
    private var buttonPosition: Int = 0
    private var evidenceNumber = 0
    private var prizeWebId = 0L
    private var synchronizeStarted = false
    private lateinit var itemStatus: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prizes)

        Realm.init(this)

        setListenerToViews()
    }

    override fun onResume() {
        showPrizes()
        super.onResume()

        if (synchronizeStarted) {
            UtilHelper().showView(prizes_progress, true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.prizes, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_synchronize) {
            UtilHelper().showView(prizes_progress, true)
            itemStatus = item
            item.isEnabled = false
            isSessionActive()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            PermissionsHelper.REQUEST_PERMISSIONS ->
                // If request is cancelled, the result arrays are empty.
                if (PermissionsHelper(this).validatePermissionResult(grantResults)) {
                    goToScannerScreen()
                } else {
                    Log.e(tag, "Denied permissions :(")

                    val snack = Snackbar.make(prizes_layout,
                            R.string.error_missing_permissions,
                            Snackbar.LENGTH_LONG)
                    UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_WARNING)
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            if (requestCode == ScannerActivity.REQUEST_CODE_CAMERA) {
                try {
                    val code = data!!.extras.getString(ScannerActivity.EXTRA_RESULT_CODE)
                    tryToFindMachine(code)
                } catch (ex: Exception) {
                    Log.e(tag, "Attempting to get extras from result", ex)
                    Rollbar.instance().error(ex, tag)
                }
            }
        }
    }

    private fun getNotSyncedPrize(): List<Prize> {
        val allPrizes = PrizeDao().findAll(PreferencesHelper(this).userId)
        return allPrizes.filter { !it.hasSynchronizedData }
    }

    private fun getNotSyncedPrizes(): List<Prize> {
        val allPrizes = PrizeDao().findAll(PreferencesHelper(this).userId)
        val notSyncedPrizes = allPrizes.filter { !it.hasSynchronizedData }
        return notSyncedPrizes.filter { it.webId == 0L }
    }

    private fun getPrizeEvidence(): List<Evidence> {
        val allEvidences = EvidenceDao()
                .findAllByType(EVIDENCE_TYPE, PreferencesHelper(this).userId)
        val evidencesNotSynchronized = allEvidences.filter { !it.isSynchronized }
        return evidencesNotSynchronized.filter { it.evidenceableId == prizeWebId }
    }

    private fun getNotSyncedEvidences(): List<Evidence> {
        val allEvidences = EvidenceDao()
                .findAllByType(EVIDENCE_TYPE, PreferencesHelper(this).userId)
        return allEvidences.filter { !it.isSynchronized }
    }

    private fun isSessionActive() {
        if (!HttpClientService.sessionUnauthorized) {
            tryToSynchronizePrizes()
        } else {
            UtilHelper().showView(prizes_progress, false)
            itemStatus.isEnabled = true
            LogoutHelper().tryToLogout(this)
        }
    }

    private fun tryToSynchronizePrizes() {
        val notSyncedPrizes = getNotSyncedPrizes()
        val notSyncedEvidences = getNotSyncedEvidences()

        when {
            notSyncedPrizes.isNotEmpty() -> {
                val formattedPrize = getFormattedPrizes(notSyncedPrizes[0])

                val syncing = SynchronizePrizesService(this)
                        .synchronizePrizes(formattedPrize)

                validateSynchronization(syncing)
                evidenceNumber = 0
            }
            notSyncedEvidences.isNotEmpty() -> {
                prizeWebId = 0L
                tryToSynchronizeEvidences()
                setStatusPrizes()
            }
            else -> {
                if (synchronizeStarted) {
                    // In case that some prize status couldn't change, change it
                    setStatusPrizes()
                    UtilHelper().showView(prizes_progress, false)

                    val snack = Snackbar.make(prizes_layout,
                            getString(R.string.prizes_synced), Snackbar.LENGTH_LONG)
                    UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_SUCCESS)
                    synchronizeStarted = false
                    itemStatus.isEnabled = true
                } else {
                    UtilHelper().showView(prizes_progress, false)

                    val snack = Snackbar.make(prizes_layout,
                            getString(R.string.prizes_not_pending), Snackbar.LENGTH_LONG)
                    UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_WARNING)
                    itemStatus.isEnabled = true
                }
            }
        }
    }

    private fun notifySynchronizationStarted() {
        if (!synchronizeStarted) {
            val snack = Snackbar.make(prizes_layout,
                    getString(R.string.prizes_syncing), Snackbar.LENGTH_LONG)
            UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_WARNING)
            synchronizeStarted = true
        }
    }

    private fun getFormattedPrizes(prize: Prize): JSONObject {
        val prizes = JSONArray()

        val date = prize.createdAt
        val prizeDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                Locale.getDefault()).format(date)

        val prizeJson = JSONObject()
                .put("user_id", prize.userId)
                .put("game_machine_id", prize.gameMachineId)
                .put("input_reading", prize.inputReading)
                .put("output_reading", prize.outputReading)
                .put("screen_number", prize.screen)
                .put("prize_amount", prize.prizeAmount)
                .put("current_amount", prize.currentAmount)
                .put("to_complete", prize.toComplete)
                .put("game_machine_fund", prize.gameMachineFund)
                .put("expense_amount", prize.expenseAmount)
                .put("week_number", prize.week)
                .put("comments", prize.comments)
                .put("created_at", prizeDate)
                .put("mobile_id", prize.id)

        prizes.put(prizeJson)

        val prizeArray = JSONObject()
                .put("prizes_array", prizes)

        return JSONObject()
                .put("prizes", prizeArray)
    }

    private fun tryToSynchronizeEvidences() {
        val noSyncedEvidence: List<Evidence> = if (prizeWebId == 0L) {
            // When the prizes are synced the variable can't change then get all the
            // evidences that are not synced and try to synchronize them
            getNotSyncedEvidences()
        } else {
            getPrizeEvidence()
        }

        if (noSyncedEvidence.isNotEmpty()) {
            val item = JSONObject()

            val fileDescription = JSONObject()
                    .put("file", noSyncedEvidence[0].file)
                    .put("filename", noSyncedEvidence[0].filename)
                    .put("original_filename", noSyncedEvidence[0].originalFilename)

            val file = JSONObject()
                    .put("evidenceable_id", noSyncedEvidence[0].evidenceableId)
                    .put("evidenceable_type", noSyncedEvidence[0].evidenceableType)
                    .put("file", fileDescription)

            item.put("evidence", file)

            val syncing = SynchronizeEvidencesService(this)
                    .synchronizeEvidences(item, noSyncedEvidence[0].id)

            validateSynchronization(syncing)
            evidenceNumber++
        }
    }

    private fun validateSynchronization(syncing: Boolean) {
        if (syncing) {
            notifySynchronizationStarted()
        } else {
            UtilHelper().showView(prizes_progress, false)

            synchronizeStarted = false
            val snack = Snackbar.make(prizes_layout,
                    getString(R.string.error_no_internet_connection), Snackbar.LENGTH_LONG)
            UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
            itemStatus.isEnabled = true
        }
    }

    private fun getGameMachines(): RealmResults<GameMachine> {
        return GameMachineDao().findAll()
    }

    private fun getPrizes(): RealmResults<Prize> {
        return PrizeDao().findAll(PreferencesHelper(this).userId)
    }

    private fun setDonePrizesNumber() {
        val gameMachines = getGameMachines().size
        val prizes = getPrizes().size
        val donePrizes = "$prizes/$gameMachines " + getString(R.string.prizes_given_prizes)
        prizes_text_number.text = donePrizes
    }

    private fun addItemsToListView() {
        val prizes = getPrizes()

        if (prizes.size != 0) {
            prizes_list.adapter = PrizesListAdapter(this, prizes)
            prizes_text_no_prizes.visibility = View.GONE
        }
    }

    private fun showPrizes() {
        UtilHelper().showView(prizes_progress, true)

        setDonePrizesNumber()
        addItemsToListView()

        UtilHelper().showView(prizes_progress, false)
    }

    private fun goToScannerScreen() {
        val intent = Intent(this, ScannerActivity::class.java)
        startActivityForResult(intent, ScannerActivity.REQUEST_CODE_CAMERA)
        AnimationHelper().enterTransition(this)
    }

    private fun goToFormScreen(code: String) {
        val intent = PrizeFormActivity.getStartIntent(this, code)
        startActivity(intent)
        AnimationHelper().enterTransition(this)
    }

    private fun tryToFindMachine(code: String) {
        val found = validateCode(code)

        if (found) {
            Log.d(tag, "------> Game machine was found!")
            goToFormScreen(code)
        } else {
            val snack = Snackbar.make(prizes_layout,
                    R.string.error_unregistered_code,
                    Snackbar.LENGTH_LONG)
            UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
        }
    }

    private fun validatePermissions() {
        val hasPermissions = PermissionsHelper(this).requestAllPermissions()

        if (hasPermissions) {
            goToScannerScreen()
        }
    }

    private fun validateCode(code: String): Boolean {
        val routeCode = RouteDao().findAll().first()?.code

        return getGameMachines()
                .map { "$routeCode-${it.folio}" }
                .contains(code)
    }

    private fun validateEvidenceNumber() {
        when (evidenceNumber) {
            1 -> {
                tryToSynchronizeEvidences()
            }
            else -> {
                setStatusPrizes()
                isSessionActive()
            }
        }
    }

    private fun setStatusPrizes() {
        val prizes = if (prizeWebId != 0L) {
            getNotSyncedPrize().filter { it.webId == prizeWebId }
        } else {
            getNotSyncedPrize()
        }

        if (prizes.isNotEmpty()) {
            for (revision in prizes) {
                val prizeObject = PrizeDao().findCopyById(revision.id)
                val evidence = prizeObject.evidences

                if (evidence[0]?.isSynchronized == true && evidence[1]?.isSynchronized == true) {
                    prizeObject.hasSynchronizedData = true
                    PrizeDao().update(prizeObject)
                }
            }
        }
        setDonePrizesNumber()
        addItemsToListView()
    }

    private fun setListenerToViews() {
        prizes_fab.setOnClickListener {
            if (StorageAccess().checkInternalStorageAvailable()) {
                validatePermissions()
            } else {
                UtilHelper().showStorageAvailableAlert(this)
            }
        }

        prizes_list.setOnScrollListener(object : RecyclerView.OnScrollListener(),
                AbsListView.OnScrollListener {
            override fun onScroll(p0: AbsListView?, p1: Int, p2: Int, p3: Int) {
                if (p1 == 0) {
                    prizes_fab.visibility = View.VISIBLE
                } else {
                    if (p1 != buttonPosition) {
                        prizes_fab.visibility = View.GONE
                        buttonPosition = p1
                    }
                }
            }

            override fun onScrollStateChanged(p0: AbsListView?, p1: Int) {
                prizes_fab.visibility = View.VISIBLE
            }
        })
    }

    override fun onPrizeSuccess(webId: Long) {
        prizeWebId = webId
        tryToSynchronizeEvidences()
    }

    override fun onPrizeFailure(error: String) {
        Log.e(tag, "Prize failed: $error")
        UtilHelper().showView(prizes_progress, false)

        val snack = Snackbar.make(prizes_layout,
                getString(R.string.error_default), Snackbar.LENGTH_LONG)
        UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
        itemStatus.isEnabled = true
    }

    override fun onEvidenceSuccess() {
        validateEvidenceNumber()
    }

    override fun onEvidenceFailure(error: String) {
        Log.e(tag, "Evidence failed: $error")
        // If one evidence fails, continue synchronizing the others
        validateEvidenceNumber()
    }
}