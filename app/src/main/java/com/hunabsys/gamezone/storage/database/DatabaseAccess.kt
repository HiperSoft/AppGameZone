package com.hunabsys.gamezone.storage.database

import android.app.AlertDialog
import android.content.Context
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.PreferencesHelper
import com.hunabsys.gamezone.models.daos.CheckInDao
import com.hunabsys.gamezone.models.daos.ExpenseDao
import com.hunabsys.gamezone.models.daos.PrizeDao
import com.hunabsys.gamezone.models.daos.SaleRevisionDao
import com.hunabsys.gamezone.models.datamodels.CheckIn
import com.hunabsys.gamezone.models.datamodels.Expense
import com.hunabsys.gamezone.models.datamodels.Prize
import com.hunabsys.gamezone.models.datamodels.SaleRevision

class DatabaseAccess {

    companion object {
        private var hasPendingCheckIns: Boolean = false
        private var hasPendingSaleRevisions: Boolean = false
        private var hasPendingPrizes: Boolean = false
        private var hasPendingExpenses: Boolean = false
    }

    fun hasPendingData(context: Context): Boolean {
        val pendingCheckIns = getAllCheckIns(context).filter { !it.isSynchronized }
        hasPendingCheckIns = pendingCheckIns.isNotEmpty()

        val pendingRevisions = getAllSaleRevisions(context).filter { !it.hasSynchronizedData }
        hasPendingSaleRevisions = pendingRevisions.isNotEmpty()

        val pendingPrizes = getAllPrizes(context).filter { !it.hasSynchronizedData }
        hasPendingPrizes = pendingPrizes.isNotEmpty()

        val pendingExpenses = getAllExpenses(context).filter { !it.isSynchronized }
        hasPendingExpenses = pendingExpenses.isNotEmpty()

        return (hasPendingCheckIns
                || hasPendingSaleRevisions
                || hasPendingPrizes
                || hasPendingExpenses)
    }

    private fun getAllCheckIns(context: Context): List<CheckIn> {
        return CheckInDao().findAll(PreferencesHelper(context).userId)
    }

    private fun getAllSaleRevisions(context: Context): List<SaleRevision> {
        return SaleRevisionDao().findAll(PreferencesHelper(context).userId)
    }

    private fun getAllPrizes(context: Context): List<Prize> {
        return PrizeDao().findAll(PreferencesHelper(context).userId)
    }

    private fun getAllExpenses(context: Context): List<Expense> {
        return ExpenseDao().findAll(PreferencesHelper(context).userId)
    }

    private fun getWarningMessage(context: Context): String {
        val explanation = context.getString(R.string.close_week_pending_data)
        var message = context.getString(R.string.close_week_pending) + "\n"

        if (hasPendingCheckIns) {
            message += "\n* " + context.getString(R.string.check_in_title)
        }
        if (hasPendingSaleRevisions) {
            message += "\n* " + context.getString(R.string.sale_revisions_title)
        }
        if (hasPendingPrizes) {
            message += "\n* " + context.getString(R.string.prizes_title)
        }
        if (hasPendingExpenses) {
            message += "\n* " + context.getString(R.string.expenses_title)
        }

        return message + "\n\n$explanation"
    }

    fun tryToFindNoSyncedItems(context: Context) {
        val message = getWarningMessage(context)
        AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(R.string.action_accept) { dialog, which ->
                    dialog.dismiss()
                }.show()
    }
}