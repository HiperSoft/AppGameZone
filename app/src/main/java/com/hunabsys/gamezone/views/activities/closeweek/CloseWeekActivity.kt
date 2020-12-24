package com.hunabsys.gamezone.views.activities.closeweek

/* ktlint-disable no-wildcard-imports */
import android.app.AlertDialog
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.Snackbar.make
import android.view.*
import android.widget.PopupWindow
import android.widget.RelativeLayout
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.AnimationHelper
import com.hunabsys.gamezone.helpers.LogoutHelper
import com.hunabsys.gamezone.helpers.PreferencesHelper
import com.hunabsys.gamezone.helpers.UtilHelper
import com.hunabsys.gamezone.models.daos.*
import com.hunabsys.gamezone.models.datamodels.Expense
import com.hunabsys.gamezone.models.datamodels.Prize
import com.hunabsys.gamezone.models.datamodels.SaleRevision
import com.hunabsys.gamezone.pojos.ItemCloseWeek
import com.hunabsys.gamezone.pojos.ItemCloseWeekSummary
import com.hunabsys.gamezone.services.delegates.ICloseWeekDelegate
import com.hunabsys.gamezone.services.mappers.CloseWeekMapper
import com.hunabsys.gamezone.services.rest.HttpClientService
import com.hunabsys.gamezone.services.rest.SynchronizeCloseWeekService
import com.hunabsys.gamezone.storage.database.DatabaseAccess
import com.hunabsys.gamezone.storage.storage.StorageAccess
import com.hunabsys.gamezone.views.activities.BaseActivity
import com.hunabsys.gamezone.views.activities.MainActivity
import com.hunabsys.gamezone.views.adapters.CloseWeekListAdapter
import com.hunabsys.gamezone.views.adapters.CloseWeekSummaryListAdapter
import kotlinx.android.synthetic.main.activity_close_week.*
import kotlinx.android.synthetic.main.activity_close_week_summary.view.*
import org.json.JSONObject

class CloseWeekActivity : BaseActivity(), ICloseWeekDelegate {

    private val tag = CloseWeekActivity::class.java.simpleName

    private var gameMachineOutput = 0
    private var commissionAmount = 0
    private var totalSale = 0
    private var totalPrize = 0
    private var totalExpense = 0

    private var layoutView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_close_week)

        layoutView = findViewById<RelativeLayout>(R.id.close_week_layout)
        addItemsToListView()
        setListenerForSendButton()
    }

    override fun onSupportNavigateUp(): Boolean {
        UtilHelper().showExitDialog(this)
        return true
    }

    override fun onBackPressed() {
        UtilHelper().showExitDialog(this)
    }

    private fun getAllSaleRevisions(): List<SaleRevision> {
        return SaleRevisionDao().findAll(PreferencesHelper(this).userId)
    }

    private fun getAllPrizes(): List<Prize> {
        return PrizeDao().findAll(PreferencesHelper(this).userId)
    }

    private fun getAllExpenses(): List<Expense> {
        return ExpenseDao().findAll(PreferencesHelper(this).userId)
    }

    private fun addItemsToListView() {
        val rows = getRows()
        close_week_list.adapter = CloseWeekListAdapter(this, rows)
    }

    private fun getRows(): ArrayList<ItemCloseWeek> {
        val rows = ArrayList<ItemCloseWeek>()

        var row = getTotalSaleRow(ICloseWeekConstants.TOTAL_SALE)
        rows.add(row)

        row = getTotalPrizeRow(ICloseWeekConstants.PRIZES)
        rows.add(row)

        row = getTotalExpenseRow(ICloseWeekConstants.EXPENSES)
        rows.add(row)

        row = getToDepositRow(ICloseWeekConstants.TO_DEPOSIT)
        rows.add(row)

        return rows
    }

    private fun getTotalSaleRow(position: Int): ItemCloseWeek {
        val labels = resources.getStringArray(R.array.close_week)
        totalSale = getTotalSale(getAllSaleRevisions())
        return ItemCloseWeek(labels[position], totalSale)
    }

    private fun getTotalSale(revisions: List<SaleRevision>): Int {
        var totalSale = 0

        for (revision in revisions) {
            totalSale += (revision.gameMachineOutcome.toInt() - revision.commissionAmount.toInt())
            gameMachineOutput += revision.gameMachineOutcome.toInt()
            commissionAmount += revision.commissionAmount.toInt()
        }
        return totalSale
    }

    private fun getTotalPrizeRow(position: Int): ItemCloseWeek {
        val labels = resources.getStringArray(R.array.close_week)
        totalPrize = getTotalPrize(getAllPrizes())
        return ItemCloseWeek(labels[position], totalPrize)
    }

    private fun getTotalPrize(prizes: List<Prize>): Int {
        var totalPrize = 0

        for (prize in prizes) {
            totalPrize += prize.prizeAmount.toInt()
        }

        return totalPrize
    }

    private fun getTotalExpenseRow(position: Int): ItemCloseWeek {
        val labels = resources.getStringArray(R.array.close_week)
        totalExpense = getTotalExpense(getAllExpenses())
        return ItemCloseWeek(labels[position], totalExpense)
    }

    private fun getTotalExpense(expenses: List<Expense>): Int {
        var totalExpense = 0

        for (expense in expenses) {
            totalExpense += expense.amount.toInt()
        }
        return totalExpense
    }

    private fun getToDepositRow(position: Int): ItemCloseWeek {
        val labels = resources.getStringArray(R.array.close_week)
        return ItemCloseWeek(labels[position], (totalSale - (totalPrize + totalExpense)))
    }

    private fun isWeekDataEmpty(): Boolean {
        val checkIns = CheckInDao().findAll(PreferencesHelper(this).userId)
        val saleRevisions = SaleRevisionDao().findAll(PreferencesHelper(this).userId)
        val prizes = PrizeDao().findAll(PreferencesHelper(this).userId)
        val expenses = ExpenseDao().findAll(PreferencesHelper(this).userId)

        return checkIns.isEmpty() && saleRevisions.isEmpty() &&
                prizes.isEmpty() && expenses.isEmpty()
    }

    private fun deleteAllItems() {
        CheckInDao().deleteAll(PreferencesHelper(this).userId)
        SaleRevisionDao().deleteAll(PreferencesHelper(this).userId)
        PrizeDao().deleteAll(PreferencesHelper(this).userId)
        ExpenseDao().deleteAll(PreferencesHelper(this).userId)
        EvidenceDao().deleteAll(PreferencesHelper(this).userId)
    }

    private fun getFormattedData(): JSONObject {
        val userId = PreferencesHelper(this).userId

        return JSONObject().put("user_id", userId)
    }

    private fun tryToFinishWeek() {
        val syncing = SynchronizeCloseWeekService(this)
                .synchronizeCloseWeek(getFormattedData())

        if (syncing) {
            UtilHelper().showView(close_week_progress, true)
        } else {
            val snack = make(close_week_layout,
                    getString(R.string.error_no_internet_connection), Snackbar.LENGTH_LONG)
            UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
            close_week_button_send.isClickable = true
        }
    }

    private fun setListenerForSendButton() {
        close_week_button_send.setOnClickListener {
            close_week_button_send.isClickable = false
            if (!HttpClientService.sessionUnauthorized) {
                if (isWeekDataEmpty()) {
                    val message = this.getString(R.string.close_week_nothing_captured)
                    AlertDialog.Builder(this)
                            .setMessage(message)
                            .setPositiveButton(R.string.close_week_title) { dialog, which ->
                                tryToFindSyncedItems()
                            }
                            .setNegativeButton(R.string.action_cancel) { dialog, which ->
                                close_week_button_send.isClickable = true
                                dialog.dismiss()
                            }.show()
                } else {
                    tryToFindSyncedItems()
                    close_week_button_send.isClickable = true
                }
            } else {
                close_week_button_send.isClickable = true
                LogoutHelper().tryToLogout(this)
            }
        }
    }

    private fun tryToFindSyncedItems() {
        if (DatabaseAccess().hasPendingData(this)) {
            DatabaseAccess().tryToFindNoSyncedItems(this)
        } else {
            tryToFinishWeek()
        }
    }

    private fun showDialog() {
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.activity_close_week_summary, null)

        val row = getSummaryRows()
        popupView.close_week_summary_list.adapter = CloseWeekSummaryListAdapter(this, row)

        val popupWindow = PopupWindow(
                popupView,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT)

        setListenersToDialogButtons(popupWindow, popupView)

        popupWindow.showAtLocation(layoutView, Gravity.CENTER, 0, 0)
    }

    private fun setListenersToDialogButtons(popupWindow: PopupWindow, popupView: View) {
        val buttonAccept = popupView.close_week_button_accept

        buttonAccept.setOnClickListener {
            deleteAllItems()
            StorageAccess().deleteAllImageFiles(tag, this)
            popupWindow.dismiss()
            MainActivity.routeState = 1
            closeActivity()
        }
    }

    private fun getQuantities(): HashMap<Int, String> {
        val quantities = CloseWeekMapper.getGameMachineClosed()

        if (!quantities.isEmpty()) {
            return quantities
        }
        return HashMap()
    }

    private fun getSummaryRows(): ArrayList<ItemCloseWeekSummary> {
        val rows = ArrayList<ItemCloseWeekSummary>()
        val size = getQuantities()
        var state = ""
        val machineName = this.getString(R.string.close_week_machine)

        for ((gameMachineId, weekState) in size) {
            state = when (weekState) {
                "null" -> "0" // Actual week
                "true" -> "1" // Close week
                else -> "2" // Can't close week
            }
            rows.add(ItemCloseWeekSummary("$machineName: $gameMachineId", state))
        }
        return rows
    }

    override fun onCloseWeekSuccess() {
        UtilHelper().showView(close_week_progress, false)
        showDialog()
        close_week_button_send.isClickable = true
    }

    override fun onCloseWeekFailure(error: String) {
        UtilHelper().showView(close_week_progress, false)

        val snack = make(close_week_layout,
                getString(R.string.close_week_failure),
                Snackbar.LENGTH_LONG)

        UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
        close_week_button_send.isClickable = true
    }

    private fun closeActivity() {
        finish()
        AnimationHelper().exitTransition(this)
    }
}
