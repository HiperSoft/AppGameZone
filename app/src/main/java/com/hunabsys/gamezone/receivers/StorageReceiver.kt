package com.hunabsys.gamezone.receivers
/* ktlint-disable no-wildcard-imports */
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hunabsys.gamezone.helpers.PreferencesHelper
import com.hunabsys.gamezone.models.daos.*

class StorageReceiver : BroadcastReceiver() {

    private val tag = StorageReceiver::class.java.simpleName
    val STORAGE_DELETE_ACTION = "com.hunabsys.gamezone.STORAGE_DELETE_ACTION"

    override fun onReceive(context: Context, intent: Intent) {
        try {
            deleteOldInformationOfUser(context)
        } catch (ex: Exception) {
            Log.e(tag, "Atteping to deleted storage")
        }
    }

    private fun deleteOldInformationOfUser(context: Context) {
        val userId = PreferencesHelper(context).userId

        CheckInDao().deleteAllOtherUsers(userId)
        SaleRevisionDao().deleteAllOtherUsers(userId)
        PrizeDao().deleteAllOtherUsers(userId)
        ExpenseDao().deleteAllOtherUsers(userId)
        EvidenceDao().deleteAllOtherUsers(userId)
    }
}
