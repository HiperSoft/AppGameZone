package com.hunabsys.gamezone.views.activities.salerevisions

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
import com.hunabsys.gamezone.models.daos.RouteDao
import com.hunabsys.gamezone.models.daos.SaleRevisionDao
import com.hunabsys.gamezone.models.datamodels.Evidence
import com.hunabsys.gamezone.models.datamodels.GameMachine
import com.hunabsys.gamezone.models.datamodels.SaleRevision
import com.hunabsys.gamezone.services.delegates.IEvidenceDelegate
import com.hunabsys.gamezone.services.delegates.ISaleRevisionDelegate
import com.hunabsys.gamezone.services.rest.HttpClientService
import com.hunabsys.gamezone.services.rest.SynchronizeEvidencesService
import com.hunabsys.gamezone.services.rest.SynchronizeSaleRevisionsService
import com.hunabsys.gamezone.storage.storage.StorageAccess
import com.hunabsys.gamezone.views.activities.BaseActivity
import com.hunabsys.gamezone.views.activities.ScannerActivity
import com.hunabsys.gamezone.views.activities.salerevisions.ISaleRevisionConstants.Companion.EVIDENCE_TYPE
import com.hunabsys.gamezone.views.adapters.SaleRevisionsListAdapter
import com.rollbar.android.Rollbar
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.android.synthetic.main.activity_sale_revisions.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class SaleRevisionsActivity : BaseActivity(), ISaleRevisionDelegate, IEvidenceDelegate {

    private val tag = SaleRevisionsActivity::class.java.simpleName
    private var buttonPosition: Int = 0
    private var evidenceNumber = 0
    private var saleRevisionWebId = 0L
    private var synchronizeStarted = false
    private lateinit var itemStatus: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sale_revisions)

        Realm.init(this)

        setListenersToViews()
    }

    override fun onResume() {
        showSaleRevisions()
        super.onResume()

        if (synchronizeStarted) {
            UtilHelper().showView(sale_revisions_progress, true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.sale_revisions, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_synchronize) {
            UtilHelper().showView(sale_revisions_progress, true)
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

                    val snack = Snackbar.make(sale_revisions_layout,
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

    private fun getNotSyncedRevision(): List<SaleRevision> {
        val allRevisions = SaleRevisionDao()
                .findAll(PreferencesHelper(this).userId)
        return allRevisions.filter { !it.hasSynchronizedData }
    }

    private fun getNotSyncedRevisions(): List<SaleRevision> {
        val allRevisions = SaleRevisionDao()
                .findAll(PreferencesHelper(this).userId)
        val notSynchronizedRevisions = allRevisions
                .filter { !it.hasSynchronizedData }
        return notSynchronizedRevisions.filter { it.webId == 0L }
    }

    private fun getRevisionEvidences(): List<Evidence> {
        val allEvidences = EvidenceDao()
                .findAllByType(EVIDENCE_TYPE, PreferencesHelper(this).userId)
        val evidencesNotSynchronized = allEvidences.filter { !it.isSynchronized }
        return evidencesNotSynchronized.filter { it.evidenceableId == saleRevisionWebId }
    }

    private fun getNotSyncedEvidences(): List<Evidence> {
        val allEvidences = EvidenceDao()
                .findAllByType(EVIDENCE_TYPE, PreferencesHelper(this).userId)
        return allEvidences.filter { !it.isSynchronized }
    }

    private fun isSessionActive() {
        if (!HttpClientService.sessionUnauthorized) {
            tryToSynchronizeRevisions()
        } else {
            UtilHelper().showView(sale_revisions_progress, false)
            itemStatus.isEnabled = true
            LogoutHelper().tryToLogout(this)
        }
    }

    private fun tryToSynchronizeRevisions() {
        val notSyncedRevisions = getNotSyncedRevisions()
        val notSyncedEvidences = getNotSyncedEvidences()

        when {
            notSyncedRevisions.isNotEmpty() -> {
                val formattedRevision =
                        getFormattedRevisions(notSyncedRevisions[0])

                val syncing = SynchronizeSaleRevisionsService(this)
                        .synchronizeRevisions(formattedRevision)

                validateSynchronization(syncing)
                evidenceNumber = 0
            }
            notSyncedEvidences.isNotEmpty() -> {
                saleRevisionWebId = 0L
                tryToSynchronizeEvidences()
                setStatusRevisions()
            }
            else -> {
                if (synchronizeStarted) {
                    // In case that some sale revision status couldn't change, change it
                    setStatusRevisions()
                    UtilHelper().showView(sale_revisions_progress, false)

                    val snack = Snackbar.make(sale_revisions_layout,
                            getString(R.string.sale_revisions_synced), Snackbar.LENGTH_LONG)
                    UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_SUCCESS)
                    synchronizeStarted = false
                    itemStatus.isEnabled = true
                } else {
                    UtilHelper().showView(sale_revisions_progress, false)

                    val snack = Snackbar.make(sale_revisions_layout,
                            getString(R.string.sale_revisions_not_pending), Snackbar.LENGTH_LONG)
                    UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_WARNING)

                    itemStatus.isEnabled = true
                }
            }
        }
    }

    private fun notifySynchronizationStarted() {
        if (!synchronizeStarted) {
            val snack = Snackbar.make(sale_revisions_layout,
                    getString(R.string.sale_revisions_syncing), Snackbar.LENGTH_LONG)
            UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_WARNING)
            synchronizeStarted = true
        }
    }

    private fun getFormattedRevisions(saleRevision: SaleRevision): JSONObject {
        val revisions = JSONArray()

        val latLng = ArrayList<Double>()
        latLng.add(saleRevision.longitude)
        latLng.add(saleRevision.latitude)

        val coordinatesArray = JSONArray(latLng)

        val geometry = JSONObject()
                .put("type", "Point")
                .put("coordinates", coordinatesArray)

        val coordinates = JSONObject()
                .put("type", "Feature")
                .put("geometry", geometry)

        val location = JSONObject()
                .put("game_machine_id", saleRevision.gameMachineId)
                .put("coordinates", coordinates)

        val date = saleRevision.createdAt
        val revisionDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                Locale.getDefault()).format(date)

        val revision = JSONObject()
                .put("route_id", saleRevision.routeId)
                .put("point_of_sale_id", saleRevision.pointOfSaleId)
                .put("game_machine_id", saleRevision.gameMachineId)
                .put("week", saleRevision.week)
                .put("screen_number", saleRevision.screen)
                .put("input_read", saleRevision.entry)
                .put("output_read", saleRevision.outcome)
                .put("game_machine_output", saleRevision.gameMachineOutcome)
                .put("sales_commission_percentage", saleRevision.commissionPercentage)
                .put("sales_commission_amount", saleRevision.commissionAmount)
                .put("game_machine_fund", saleRevision.currentFund)
                .put("comments", saleRevision.comments)
                .put("created_at", revisionDate)
                .put("mobile_id", saleRevision.id)
                .put("game_machine_historic_locations_attributes", location)

        revisions.put(revision)

        val revisionsArray = JSONObject()
                .put("sales_array", revisions)

        return JSONObject()
                .put("revisions", revisionsArray)
    }

    private fun tryToSynchronizeEvidences() {
        val noSyncedEvidence: List<Evidence> = if (saleRevisionWebId == 0L) {
            // When the sale revisions are synced the variable can't change then get all the
            // evidences that are not synced and try to synchronize them
            getNotSyncedEvidences()
        } else {
            getRevisionEvidences()
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
            UtilHelper().showView(sale_revisions_progress, false)

            synchronizeStarted = false
            val snack = Snackbar.make(sale_revisions_layout,
                    getString(R.string.error_no_internet_connection), Snackbar.LENGTH_LONG)
            UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
            itemStatus.isEnabled = true
        }
    }

    private fun getGameMachines(): RealmResults<GameMachine> {
        return GameMachineDao().findAll()
    }

    private fun getSaleRevisions(): RealmResults<SaleRevision> {
        return SaleRevisionDao().findAll(PreferencesHelper(this).userId)
    }

    private fun setDoneRevisionsNumber() {
        val gameMachines = getGameMachines().size
        val saleRevisions = getSaleRevisions().size

        val doneRevisions = "$saleRevisions/$gameMachines " +
                getString(R.string.sale_revisions_done_revisions)
        sale_revisions_text_number.text = doneRevisions
    }

    private fun addItemsToListView() {
        val saleRevisions = getSaleRevisions()

        if (saleRevisions.size != 0) {
            sale_revisions_list.adapter = SaleRevisionsListAdapter(this, saleRevisions)
            sale_revisions_text_no_revisions.visibility = View.GONE
        }
    }

    private fun showSaleRevisions() {
        UtilHelper().showView(sale_revisions_progress, true)

        setDoneRevisionsNumber()
        addItemsToListView()

        UtilHelper().showView(sale_revisions_progress, false)
    }

    private fun goToScannerScreen() {
        val intent = Intent(this, ScannerActivity::class.java)
        startActivityForResult(intent, ScannerActivity.REQUEST_CODE_CAMERA)
        AnimationHelper().enterTransition(this)
    }

    private fun goToFormScreen(code: String) {
        val intent = SaleRevisionFormActivity.getStartIntent(this, code)
        startActivity(intent)
        AnimationHelper().enterTransition(this)
    }

    private fun validateCode(code: String): Boolean {
        val routeCode = RouteDao().findAll().first()?.code

        return getGameMachines()
                .map { "$routeCode-${it.folio}" }
                .contains(code)
    }

    private fun tryToFindMachine(code: String) {
        val found = validateCode(code)

        if (found) {
            Log.d(tag, "------> Game machine was found!")
            goToFormScreen(code)
        } else {
            val snack = Snackbar.make(sale_revisions_layout,
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

    private fun validateEvidenceNumber() {
        when (evidenceNumber) {
            1 -> {
                tryToSynchronizeEvidences()
            }
            else -> {
                setStatusRevisions()
                isSessionActive()
            }
        }
    }

    private fun setStatusRevisions() {
        val saleRevisions = if (saleRevisionWebId != 0L) {
            getNotSyncedRevision().filter { it.webId == saleRevisionWebId }
        } else {
            getNotSyncedRevision()
        }

        if (saleRevisions.isNotEmpty()) {
            for (revision in saleRevisions) {
                val saleRevisionObject = SaleRevisionDao().findCopyById(revision.id)
                val evidence = saleRevisionObject.evidences

                if (evidence[0]?.isSynchronized == true && evidence[1]?.isSynchronized == true) {
                    saleRevisionObject.hasSynchronizedData = true
                    SaleRevisionDao().update(saleRevisionObject)
                }
            }
        }
        setDoneRevisionsNumber()
        addItemsToListView()
    }

    private fun setListenersToViews() {
        sale_revisions_fab.setOnClickListener {
            if (StorageAccess().checkInternalStorageAvailable()) {
                validatePermissions()
            } else {
                UtilHelper().showStorageAvailableAlert(this)
            }
        }

        sale_revisions_list.setOnScrollListener(object : RecyclerView.OnScrollListener(),
                AbsListView.OnScrollListener {
            override fun onScroll(p0: AbsListView?, p1: Int, p2: Int, p3: Int) {
                if (p1 == 0) {
                    sale_revisions_fab.visibility = View.VISIBLE
                } else {
                    if (p1 != buttonPosition) {
                        sale_revisions_fab.visibility = View.GONE
                        buttonPosition = p1
                    }
                }
            }

            override fun onScrollStateChanged(p0: AbsListView?, p1: Int) {
                sale_revisions_fab.visibility = View.VISIBLE
            }
        })
    }

    override fun onSaleRevisionSuccess(webId: Long) {
        saleRevisionWebId = webId
        tryToSynchronizeEvidences()
    }

    override fun onSaleRevisionFailure(error: String) {
        Log.e(tag, "SaleRevision failed: $error")
        UtilHelper().showView(sale_revisions_progress, false)

        val snack = Snackbar.make(sale_revisions_layout,
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