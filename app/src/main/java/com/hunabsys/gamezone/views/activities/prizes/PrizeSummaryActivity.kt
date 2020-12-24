package com.hunabsys.gamezone.views.activities.prizes

/* ktlint-disable no-wildcard-imports */
import android.content.Context
import android.content.Intent
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
import com.hunabsys.gamezone.models.daos.PrizeDao
import com.hunabsys.gamezone.models.datamodels.Evidence
import com.hunabsys.gamezone.models.datamodels.Prize
import com.hunabsys.gamezone.pojos.ItemPrizeSummary
import com.hunabsys.gamezone.storage.storage.StorageAccess
import com.hunabsys.gamezone.views.activities.BaseActivity
import com.hunabsys.gamezone.views.adapters.PrizeSummaryListAdapter
import com.rollbar.android.Rollbar
import io.realm.RealmList
import kotlinx.android.synthetic.main.activity_prize_summary.*
import kotlinx.android.synthetic.main.partial_machine_folio_bar.view.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Prize model
 * Created by Jonathan Hernandez on 23/07/2018
 */
class PrizeSummaryActivity : BaseActivity() {

    private val tag = PrizeSummaryActivity::class.java.simpleName

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
            val intent = Intent(context, PrizeSummaryActivity::class.java)
            intent.putExtra(IPrizeConstants.EXTRA_MACHINE_FOLIO, machineFolio)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prize_summary)

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
        val quantities = PrizeFormActivity.getQuantities()

        if (!quantities.isEmpty()) {
            return quantities
        }
        return HashMap()
    }

    private fun getEncodedImages(): ArrayList<String> {
        val imageEncoded = PrizeFormActivity.getEncodedImages()

        if (!imageEncoded.isEmpty()) {
            return imageEncoded
        }
        return ArrayList()
    }

    private fun getMachineFolio(): String {
        return this.intent.extras.getString(IPrizeConstants.EXTRA_MACHINE_FOLIO)
    }

    private fun showMachineFolio() {
        val textView = prize_summary_machine_folio_bar.game_machine_bar_text_folio
        textView.text = getMachineFolio()
    }

    private fun addItemsToListView() {
        val rows = getRows()
        prize_summary_list.adapter = PrizeSummaryListAdapter(this, rows)
    }

    private fun getRow(position: Int): ItemPrizeSummary {
        val labels = resources.getStringArray(R.array.prizes_summary)
        val quantities = getQuantities()
        val quantity = quantities[position] ?: 0
        return ItemPrizeSummary(labels[position], quantity)
    }

    private fun getRows(): ArrayList<ItemPrizeSummary> {
        val rows = ArrayList<ItemPrizeSummary>()

        var row = getRow(IPrizeConstants.INPUT)
        rows.add(row)

        row = getRow(IPrizeConstants.OUTPUT)
        rows.add(row)

        row = getRow(IPrizeConstants.SCREEN)
        rows.add(row)

        row = getRow(IPrizeConstants.PRIZE)
        rows.add(row)

        row = getRow(IPrizeConstants.CURRENT_AMOUNT)
        rows.add(row)

        row = getRow(IPrizeConstants.COMPLETE)
        rows.add(row)

        row = getRow(IPrizeConstants.FUND)
        rows.add(row)

        row = getRow(IPrizeConstants.EXPENSE)
        rows.add(row)

        return rows
    }

    private fun showProgress(show: Boolean) {
        UtilHelper().showView(prize_summary_progress, show)
        prize_summary_button_save.isEnabled = !show
    }

    private fun setQuantities(prize: Prize): Boolean {
        try {
            val input = getQuantities()[IPrizeConstants.INPUT]!!
            val output = getQuantities()[IPrizeConstants.OUTPUT]!!
            val screen = getQuantities()[IPrizeConstants.SCREEN]!!
            val prizeAmount = getQuantities()[IPrizeConstants.PRIZE]!!.toDouble()
            val current = getQuantities()[IPrizeConstants.CURRENT_AMOUNT]!!.toDouble()
            val complete = getQuantities()[IPrizeConstants.COMPLETE]!!.toDouble()
            val fund = getQuantities()[IPrizeConstants.FUND]!!.toDouble()
            val expense = getQuantities()[IPrizeConstants.EXPENSE]!!.toDouble()
            val comments = prize_summary_edit_comment.text.toString()

            prize.inputReading = input
            prize.outputReading = output
            prize.screen = screen
            prize.prizeAmount = prizeAmount
            prize.currentAmount = current
            prize.toComplete = complete
            prize.gameMachineFund = fund
            prize.expenseAmount = expense
            prize.comments = comments
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to set quantities to prize object", ex)
            Rollbar.instance().error(ex, tag)
            return false
        }
        return true
    }

    private fun setData(prize: Prize): Boolean {
        try {
            val machineFolio = getMachineFolio()
                    .substring(getMachineFolio().length - 3, getMachineFolio().length)

            val gameMachine = GameMachineDao().findByFolio(machineFolio)
            val machineId = gameMachine.id
            val pointOfSale = gameMachine.pointsOfSale!!.first()!!

            val pointOfSaleId = pointOfSale.id
            val routeId = pointOfSale.routes!!.first()!!.id

            prize.userId = PreferencesHelper(this).userId
            prize.routeId = routeId
            prize.pointOfSaleId = pointOfSaleId
            prize.gameMachineId = machineId
            prize.week = gameMachine.week
            prize.hasSynchronizedData = false
            prize.createdAt = Date()
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to save ID's and other data to prize object", ex)
            Rollbar.instance().error(ex, tag)
            return false
        }
        return true
    }

    private fun setEvidences(prize: Prize): Boolean {
        val encodedImages = getEncodedImages()

        try {
            if (encodedImages.isNotEmpty()) {
                Log.e(tag, encodedImages.toString())

                val type = IPrizeConstants.EVIDENCE_TYPE
                val evidences = RealmList<Evidence>()

                var id: Long
                val userId = PreferencesHelper(this).userId
                var filename: String
                var evidence: Evidence

                for (file in encodedImages) {
                    id = EvidenceDao().getId()

                    val date = prize.createdAt
                    val evidenceDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                            Locale.getDefault()).format(date)

                    filename = "$evidenceDate$id.png"

                    evidence = Evidence(id = id, userId = userId, evidenceId = prize.id,
                            evidenceableType = type, file = file, filename = filename,
                            originalFilename = filename)

                    val savedEvidence = EvidenceDao().saveEvidence(evidence)
                    evidences.add(savedEvidence)
                }

                prize.evidences = evidences
                return true
            } else {
                Log.e(tag, "Cannot get images: EMPTY")
            }
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to create Evidence objects", ex)
            Rollbar.instance().error(ex, tag)
        }

        return false
    }

    private fun savePrize(): Boolean {
        val prize = Prize()

        val savedQuantities = setQuantities(prize)
        val savedOtherData = setData(prize)
        val savedEvidences = setEvidences(prize)

        return if (savedQuantities && savedOtherData && savedEvidences) {
            PrizeDao().create(prize)
            Log.d(tag, "Created new Prize")
            true
        } else {
            false
        }
    }

    private fun setListenerForSaveButton() {
        prize_summary_button_save.setOnClickListener {
            showProgress(true)
            val saved = savePrize()

            StorageAccess().deleteAllImageFiles(tag, this)

            if (saved) {
                val snack = Snackbar.make(prize_summary_layout,
                        R.string.message_saved_changes,
                        Snackbar.LENGTH_LONG)
                UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_SUCCESS)

                setResult(IPrizeConstants.RESULT_CODE_SUCCESS)

                Handler().postDelayed({
                    showProgress(false)
                    finish()
                    AnimationHelper().exitTransition(this)
                }, IPrizeConstants.FINISH_DELAY)
            } else {
                showProgress(false)
                val snack = Snackbar.make(prize_summary_layout,
                        R.string.error_saving,
                        Snackbar.LENGTH_LONG)
                UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
            }
        }
    }
}