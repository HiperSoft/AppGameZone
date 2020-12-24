package com.hunabsys.gamezone.views.activities.salerevisions

/* ktlint-disable no-wildcard-imports */
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.util.Log
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.AnimationHelper
import com.hunabsys.gamezone.helpers.PreferencesHelper
import com.hunabsys.gamezone.helpers.UtilHelper
import com.hunabsys.gamezone.models.daos.EvidenceDao
import com.hunabsys.gamezone.models.daos.GameMachineDao
import com.hunabsys.gamezone.models.daos.SaleRevisionDao
import com.hunabsys.gamezone.models.datamodels.Evidence
import com.hunabsys.gamezone.models.datamodels.SaleRevision
import com.hunabsys.gamezone.pojos.ItemSaleRevisionSummary
import com.hunabsys.gamezone.storage.storage.StorageAccess
import com.hunabsys.gamezone.views.activities.BaseActivity
import com.hunabsys.gamezone.views.adapters.SaleRevisionSummaryListAdapter
import com.rollbar.android.Rollbar
import io.realm.RealmList
import kotlinx.android.synthetic.main.activity_sale_revision_summary.*
import kotlinx.android.synthetic.main.partial_machine_folio_bar.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class SaleRevisionSummaryActivity : BaseActivity() {

    private val tag = SaleRevisionSummaryActivity::class.java.simpleName

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this activity using the provided parameters.
         *
         * @param context Previous activity context.
         * @param machineFolio String containing route-machine folio.
         * @return A new intent for activity SaleRevisionSummaryActivity.
         */
        fun getStartIntent(context: Context, machineFolio: String): Intent {
            val intent = Intent(context, SaleRevisionSummaryActivity::class.java)
            intent.putExtra(ISaleRevisionConstants.EXTRA_MACHINE_FOLIO, machineFolio)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sale_revision_summary)

        showMachineFolio()
        addItemsToListView()
        setListenerForSaveButton()
    }

    override fun onSupportNavigateUp(): Boolean {
        UtilHelper().showExitDialog(this)
        return true
    }

    override fun onBackPressed() {
        UtilHelper().showExitDialog(this)
    }

    private fun getQuantities(): HashMap<Int, Int> {
        val quantities = SaleRevisionFormActivity.getQuantities()

        if (!quantities.isEmpty()) {
            return quantities
        }

        return HashMap()
    }

    private fun getEncodeImage(): ArrayList<String> {
        val encodeImage = SaleRevisionFormActivity.getImageUris()

        if (!encodeImage.isEmpty()) {
            return encodeImage
        }

        return ArrayList()
    }

    private fun getMachineFolio(): String {
        return this.intent.extras.getString(ISaleRevisionConstants.EXTRA_MACHINE_FOLIO)
    }

    private fun getCurrentLocation(): Location? {
        return SaleRevisionFormActivity.getCurrentLocation()
    }

    private fun showMachineFolio() {
        val textView = sale_revision_summary_machine_folio_bar.game_machine_bar_text_folio
        textView.text = getMachineFolio()
    }

    private fun addItemsToListView() {
        val rows = getRows()

        sale_revision_summary_list.adapter = SaleRevisionSummaryListAdapter(this, rows)
    }

    private fun getRow(position: Int): ItemSaleRevisionSummary {
        val labels = resources.getStringArray(R.array.sale_revisions_summary)
        val quantities = getQuantities()
        val quantity = quantities[position] ?: 0 // Assign 0 if value is null
        return ItemSaleRevisionSummary(labels[position], quantity)
    }

    private fun getRows(): ArrayList<ItemSaleRevisionSummary> {
        val rows = ArrayList<ItemSaleRevisionSummary>()

        var row = getRow(ISaleRevisionConstants.ENTRY)
        rows.add(row)

        row = getRow(ISaleRevisionConstants.OUTCOME)
        rows.add(row)

        row = getRow(ISaleRevisionConstants.SCREEN)
        rows.add(row)

        row = getRow(ISaleRevisionConstants.GAME_MACHINE_OUTCOME)
        rows.add(row)

        row = getRow(ISaleRevisionConstants.COMMISSION_AMOUNT)
        rows.add(row)

        row = getRow(ISaleRevisionConstants.FUND)
        rows.add(row)

        return rows
    }

    private fun showProgress(show: Boolean) {
        UtilHelper().showView(sale_revision_summary_progress, show)
        sale_revision_summary_button_save.isEnabled = !show
    }

    private fun setQuantities(saleRevision: SaleRevision): Boolean {
        try {
            val entry = getQuantities()[ISaleRevisionConstants.ENTRY]!!
            val outcome = getQuantities()[ISaleRevisionConstants.OUTCOME]!!
            val screen = getQuantities()[ISaleRevisionConstants.SCREEN]!!
            val machine = getQuantities()[ISaleRevisionConstants.GAME_MACHINE_OUTCOME]!!.toDouble()
            val amount = getQuantities()[ISaleRevisionConstants.COMMISSION_AMOUNT]!!.toDouble()
            val percentage = getQuantities()[ISaleRevisionConstants.COMMISSION_PERCENTAGE]!!
            val fund = getQuantities()[ISaleRevisionConstants.FUND]!!.toDouble()
            val comments = sale_revision_summary_edit_comment.text.toString()

            saleRevision.entry = entry
            saleRevision.outcome = outcome
            saleRevision.screen = screen
            saleRevision.gameMachineOutcome = machine
            saleRevision.commissionAmount = amount
            saleRevision.commissionPercentage = percentage
            saleRevision.currentFund = fund
            saleRevision.comments = comments
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to set quantities to saleRevision object", ex)
            Rollbar.instance().error(ex, tag)
            return false
        }
        return true
    }

    private fun setData(saleRevision: SaleRevision): Boolean {
        try {
            val machineFolio = getMachineFolio()
                    .substring(getMachineFolio().length - 3, getMachineFolio().length)

            val gameMachine = GameMachineDao().findByFolio(machineFolio)
            val machineId = gameMachine.id
            val pointOfSale = gameMachine.pointsOfSale!!.first()!!

            val pointOfSaleId = pointOfSale.id
            val routeId = pointOfSale.routes!!.first()!!.id

            val currentLocation = getCurrentLocation()

            saleRevision.userId = PreferencesHelper(this).userId
            saleRevision.routeId = routeId
            saleRevision.pointOfSaleId = pointOfSaleId
            saleRevision.gameMachineId = machineId
            saleRevision.week = gameMachine.week
            saleRevision.hasSynchronizedData = false
            saleRevision.createdAt = Date()

            if (currentLocation != null) {
                saleRevision.latitude = currentLocation.latitude
                saleRevision.longitude = currentLocation.longitude
            }

            Log.e(tag, "LAT: " + saleRevision.latitude.toString() + " - LNG: " +
                    saleRevision.longitude)
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to save ID's and other data to saleRevision object", ex)
            Rollbar.instance().error(ex, tag)
            return false
        }
        return true
    }

    private fun setEvidences(saleRevision: SaleRevision): Boolean {
        val encodeImages = getEncodeImage()

        try {
            if (encodeImages.isNotEmpty()) {
                val type = ISaleRevisionConstants.EVIDENCE_TYPE
                val evidences = RealmList<Evidence>()

                var id: Long
                val userId = PreferencesHelper(this).userId
                var filename: String
                var evidence: Evidence

                for (file in encodeImages) {
                    id = EvidenceDao().getId()

                    val date = saleRevision.createdAt
                    val evidenceDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                            Locale.getDefault()).format(date)

                    filename = "$evidenceDate$id.png"

                    evidence = Evidence(id = id, userId = userId, evidenceId = saleRevision.id,
                            evidenceableType = type, file = file, filename = filename,
                            originalFilename = filename)

                    val savedEvidence = EvidenceDao().saveEvidence(evidence)
                    evidences.add(savedEvidence)
                }

                saleRevision.evidences = evidences
                return true
            } else {
                Log.e(tag, "Cannot get image uris: EMPTY")
            }
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to create Evidence objects", ex)
            Rollbar.instance().error(ex, tag)
        }
        return false
    }

    private fun saveSaleRevision(): Boolean {
        val saleRevision = SaleRevision()

        val savedQuantities = setQuantities(saleRevision)
        val savedOtherData = setData(saleRevision)
        val savedEvidences = setEvidences(saleRevision)

        if (savedQuantities && savedOtherData && savedEvidences) {
            SaleRevisionDao().create(saleRevision)
            Log.d(tag, "Created new Sale Revision")
        } else {
            return false
        }
        return true
    }

    private fun setListenerForSaveButton() {
        sale_revision_summary_button_save.setOnClickListener {
            showProgress(true)
            val saved = saveSaleRevision()

            StorageAccess().deleteAllImageFiles(tag, this)

            if (saved) {
                val snack = Snackbar.make(sale_revision_summary_layout,
                        R.string.message_saved_changes,
                        Snackbar.LENGTH_LONG)
                UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_SUCCESS)

                setResult(ISaleRevisionConstants.RESULT_CODE_SUCCESS)

                Handler().postDelayed({
                    showProgress(false)
                    finish()
                    AnimationHelper().exitTransition(this)
                }, ISaleRevisionConstants.FINISH_DELAY)
            } else {
                showProgress(false)
                val snack = Snackbar.make(sale_revision_summary_layout,
                        R.string.error_saving,
                        Snackbar.LENGTH_LONG)
                UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
            }
        }
    }
}
