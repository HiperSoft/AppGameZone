package com.hunabsys.gamezone.views.activities

/* ktlint-disable no-wildcard-imports */
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.AnimationHelper
import com.hunabsys.gamezone.helpers.LogoutHelper
import com.hunabsys.gamezone.helpers.PreferencesHelper
import com.hunabsys.gamezone.helpers.UtilHelper
import com.hunabsys.gamezone.models.daos.*
import com.hunabsys.gamezone.receivers.StorageReceiver
import com.hunabsys.gamezone.services.delegates.IRouteConfigurationDelegate
import com.hunabsys.gamezone.services.rest.HttpClientService
import com.hunabsys.gamezone.services.rest.RouteConfigurationService
import com.hunabsys.gamezone.storage.database.DatabaseAccess
import com.hunabsys.gamezone.storage.storage.StorageAccess
import com.hunabsys.gamezone.views.activities.checkins.CheckInActivity
import com.hunabsys.gamezone.views.activities.expenses.ExpensesActivity
import com.hunabsys.gamezone.views.activities.prizes.PrizesActivity
import com.hunabsys.gamezone.views.activities.salerevisions.SaleRevisionsActivity
import com.rollbar.android.Rollbar
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_close_week.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

const val STORE_DELETE_CODE = 1

class MainActivity : AppCompatActivity(), IRouteConfigurationDelegate {

    private val tag = MainActivity::class.simpleName
    private var alertDialog: AlertDialog? = null
    private lateinit var storageReceiver: StorageReceiver

    companion object {
        var routeState: Int = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Realm.init(this)

        Rollbar.init(this)
        Rollbar.instance().setPersonData(PreferencesHelper(this).userId.toString(),
                PreferencesHelper(this).userName, PreferencesHelper(this).email)

        getConfiguration()
        setListenersToViews()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here.
        // The action bar will automatically handle clicks on the Home/Up button,
        // so long as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == R.id.action_logout) {
            logout()
        }
        if (id == R.id.action_delete) {
            tryToDeleteData()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()

        if (routeState == 1) {
            PreferencesHelper(this).savedConfig = false
            getConfiguration()
            routeState = 0
        }

        setUpStorageDeleted()
    }

    override fun onBackPressed() {
        AnimationHelper().exitTransition(this)
        super.onBackPressed()
    }

    override fun onRouteConfigurationSuccess() {
        if (HttpClientService.userPointOfSaleUnsigned) {
            UtilHelper().showView(main_progress, false)
            LogoutHelper().tryToLogout(this)
        } else {
            UtilHelper().showView(main_progress, false)
        }
    }

    override fun onRouteConfigurationFailure(error: String) {
        if (HttpClientService.userRoutesUnsigned) {
            UtilHelper().showView(main_progress, false)
            LogoutHelper().tryToLogout(this)
        } else {
            UtilHelper().showView(main_progress, false)

            val snack = Snackbar.make(main_layout_container, error, Snackbar.LENGTH_LONG)
            UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
        }
    }

    private fun getConfiguration() {
        if (!PreferencesHelper(this).savedConfig) {
            val gettingConfig = RouteConfigurationService(this).getConfiguration()

            if (gettingConfig) {
                UtilHelper().showView(main_progress, true)
                main_swipe_refresh.isRefreshing = false
            } else {
                main_swipe_refresh.isRefreshing = false
                val snack = Snackbar.make(main_layout_container,
                        R.string.error_no_internet_connection,
                        Snackbar.LENGTH_LONG)
                UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
            }
        }
    }

    private fun logout() {
        if (DatabaseAccess().hasPendingData(this)) {
            DatabaseAccess().tryToFindNoSyncedItems(this)
        } else {
            if (alertDialog != null) {
                alertDialog?.cancel()
            }
            LogoutHelper().tryToLogout(this)
        }
    }

    private fun setListenersToViews() {
        main_button_check_in.setOnClickListener {
            goToNextScreen(it.id)
        }
        main_button_sale_revisions.setOnClickListener {
            goToNextScreen(it.id)
        }
        main_button_prizes.setOnClickListener {
            goToNextScreen(it.id)
        }
        main_button_expenses.setOnClickListener {
            goToNextScreen(it.id)
        }
        //main_button_finish_week.setOnClickListener {
        //goToNextScreen(it.id)
        //}

        main_swipe_refresh.setOnRefreshListener {
            if (!HttpClientService.sessionUnauthorized) {
                PreferencesHelper(this).savedConfig = false
                getConfiguration()
            } else {
                UtilHelper().showView(main_progress, false)
                LogoutHelper().tryToLogout(this)
            }
        }
    }

    private fun goToNextScreen(viewId: Int) {
        val intent: Intent

        when (viewId) {
            main_button_check_in.id -> {
                intent = Intent(this, CheckInActivity::class.java)
            }
            main_button_sale_revisions.id -> {
                intent = Intent(this, SaleRevisionsActivity::class.java)
            }
            main_button_prizes.id -> {
                intent = Intent(this, PrizesActivity::class.java)
            }
            main_button_expenses.id -> {
                intent = Intent(this, ExpensesActivity::class.java)
            }
//            main_button_finish_week.id -> {
            //intent = Intent(this, CloseWeekActivity::class.java)
//            }
            else -> {
                intent = Intent(this, SaleRevisionsActivity::class.java)
            }
        }

        startActivity(intent)
        AnimationHelper().enterTransition(this)
    }

    private fun tryToDeleteData() {
        if (isWeekDataEmpty()) {
            val message = this.getString(R.string.default_no_data)
            AlertDialog.Builder(this)
                    .setMessage(message)
                    .setPositiveButton(R.string.action_accept) { dialog, _ ->
                        dialog.dismiss()
                    }.show()
        } else {
            val message = this.getString(R.string.main_delete_start)
            AlertDialog.Builder(this)
                    .setMessage(message)
                    .setPositiveButton(R.string.action_accept) { _, _ ->
                        tryToFindSyncedItems()
                    }
                    .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                        dialog.dismiss()
                    }.show()
        }
    }

    private fun isWeekDataEmpty(): Boolean {
        val checkIns = CheckInDao().findAll(PreferencesHelper(this).userId)
        val saleRevisions = SaleRevisionDao().findAll(PreferencesHelper(this).userId)
        val prizes = PrizeDao().findAll(PreferencesHelper(this).userId)
        val expenses = ExpenseDao().findAll(PreferencesHelper(this).userId)

        return checkIns.isEmpty() && saleRevisions.isEmpty() &&
                prizes.isEmpty() && expenses.isEmpty()
    }

    private fun tryToFindSyncedItems() {
        if (DatabaseAccess().hasPendingData(this)) {
            DatabaseAccess().tryToFindNoSyncedItems(this)
        } else {
            try {
                deleteAllItems()
                StorageAccess().deleteAllImageFiles("MainActivity", this)
                PreferencesHelper(this).savedConfig = false
                getConfiguration()
                isSuccessDeleted()
            } catch (ex: Exception) {
                Rollbar.instance().info("$tag - Try drop information and getting " +
                        "information \n $ex")
            }
        }
    }

    private fun deleteAllItems() {
        CheckInDao().deleteAll(PreferencesHelper(this).userId)
        SaleRevisionDao().deleteAll(PreferencesHelper(this).userId)
        PrizeDao().deleteAll(PreferencesHelper(this).userId)
        ExpenseDao().deleteAll(PreferencesHelper(this).userId)
        EvidenceDao().deleteAll(PreferencesHelper(this).userId)
    }

    private fun isSuccessDeleted() {
        val message = this.getString(R.string.main_delete_done)
        android.app.AlertDialog.Builder(this).setTitle(message)
                .setPositiveButton(R.string.action_accept) { dialog, _ ->
                    dialog.dismiss()
                }.show()
    }

    private fun setUpStorageDeleted() {
        if (!PreferencesHelper(this).storageAlarm) {
            try {
                storageReceiver = StorageReceiver()

                val calendar: Calendar = Calendar.getInstance().apply {
                    timeInMillis = System.currentTimeMillis()
                    set(Calendar.HOUR_OF_DAY, 6)
                }

                val storageIntent = Intent(this, StorageReceiver::class.java)
                storageIntent.action = StorageReceiver().STORAGE_DELETE_ACTION
                sendBroadcast(storageIntent)

                val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager

                val storagePendingIntent = PendingIntent.getBroadcast(this,
                        STORE_DELETE_CODE, storageIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT)

                alarm.cancel(storagePendingIntent)

                alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY * 14, storagePendingIntent)

                PreferencesHelper(this).storageAlarm = true
            } catch (ex: Exception) {
                Log.e(tag, "Attempting to set alarm for delete storage", ex)
            }
        }
    }
}
