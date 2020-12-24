package com.hunabsys.gamezone.views.activities.checkins

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
import com.hunabsys.gamezone.models.daos.CheckInDao
import com.hunabsys.gamezone.models.daos.PointOfSaleDao
import com.hunabsys.gamezone.models.daos.RouteDao
import com.hunabsys.gamezone.models.datamodels.CheckIn
import com.hunabsys.gamezone.models.datamodels.PointOfSale
import com.hunabsys.gamezone.services.delegates.ICheckInDelegate
import com.hunabsys.gamezone.services.rest.HttpClientService
import com.hunabsys.gamezone.services.rest.SynchronizeCheckInService
import com.hunabsys.gamezone.storage.storage.StorageAccess
import com.hunabsys.gamezone.views.activities.BaseActivity
import com.hunabsys.gamezone.views.activities.ScannerActivity
import com.hunabsys.gamezone.views.adapters.CheckInListAdapter
import com.rollbar.android.Rollbar
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.android.synthetic.main.activity_check_in.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class CheckInActivity : BaseActivity(), ICheckInDelegate {

    private val tag = CheckInActivity::class.java.simpleName
    private var route = ""
    private var pointOfSaleFolio: String = ""
    private var buttonPosition: Int = 0
    private var synchronizeStarted = false
    private lateinit var itemStatus: MenuItem

    private val qrCode = ArrayList<String>(4)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_in)

        Realm.init(this)
        setCodesToArray()
        setListenersToViews()
    }

    override fun onResume() {
        showCheckIns()
        super.onResume()

        if (synchronizeStarted) {
            UtilHelper().showView(check_in_progress, true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.check_in, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_synchronize) {
            UtilHelper().showView(check_in_progress, true)
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

                    val snack = Snackbar.make(check_in_layout,
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
                    tryToFindPointOSale(code)
                } catch (ex: Exception) {
                    Log.d(tag, "Attempting to get extras from result", ex)
                    Rollbar.instance().error(ex, tag)
                }
            }
        }
    }

    private fun getNotSyncedCheckIns(): List<CheckIn> {
        val allCheckIns = CheckInDao().findAll(PreferencesHelper(this).userId)
        return allCheckIns.filter { !it.isSynchronized }
    }

    private fun getPointsOfSale(): RealmResults<PointOfSale> {
        return PointOfSaleDao().findAll()
    }

    private fun getCheckIns(): RealmResults<CheckIn> {
        return CheckInDao().findAll(PreferencesHelper(this).userId)
    }

    private fun isSessionActive() {
        if (!HttpClientService.sessionUnauthorized) {
            tryToSynchronizeCheckIn()
        } else {
            UtilHelper().showView(check_in_progress, false)
            itemStatus.isEnabled = true
            LogoutHelper().tryToLogout(this)
        }
    }

    private fun tryToSynchronizeCheckIn() {
        val notSyncedCheckIns = getNotSyncedCheckIns()

        if (notSyncedCheckIns.isNotEmpty()) {
            val syncing = SynchronizeCheckInService(this)
                    .synchronizeCheckIns(getFormattedCheckIns(notSyncedCheckIns[0]))

            validateSynchronization(syncing)
        } else {
            if (synchronizeStarted) {
                UtilHelper().showView(check_in_progress, false)

                val snack = Snackbar.make(check_in_layout,
                        getString(R.string.check_in_synced_visits), Snackbar.LENGTH_LONG)
                UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_SUCCESS)
                synchronizeStarted = false
                itemStatus.isEnabled = true
            } else {
                UtilHelper().showView(check_in_progress, false)

                val snack = Snackbar.make(check_in_layout,
                        getString(R.string.check_in_not_pending), Snackbar.LENGTH_LONG)
                UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_WARNING)
                itemStatus.isEnabled = true
            }
        }
    }

    private fun getFormattedCheckIns(checkIn: CheckIn): JSONObject {
        val checkIns = JSONArray()

        val latLng = ArrayList<Double>()
        latLng.add(checkIn.longitude)
        latLng.add(checkIn.latitude)

        val date = checkIn.createdAt
        val checkInDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                Locale.getDefault()).format(date)

        val coordinatesArray = JSONArray(latLng)
        val geometry = JSONObject()
                .put("type", "Point")
                .put("coordinates", coordinatesArray)

        val coordinates = JSONObject()
                .put("type", "Feature")
                .put("geometry", geometry)

        val checkInData = JSONObject()
                .put("mobile_id", checkIn.id)
                .put("point_of_sale_id", checkIn.pointOfSaleId)
                .put("checkin_at", checkInDate)
                .put("coordinates", coordinates)

        checkIns.put(checkInData)

        val checkInsArray = JSONObject()
                .put("checkins_array", checkIns)

        return JSONObject()
                .put("checkins", checkInsArray)
    }

    private fun notifySynchronizationStarted() {
        if (!synchronizeStarted) {
            val snack = Snackbar.make(check_in_layout,
                    getString(R.string.check_in_syncing), Snackbar.LENGTH_LONG)
            UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_WARNING)
            synchronizeStarted = true
        }
    }

    private fun validateSynchronization(syncing: Boolean) {
        if (syncing) {
            notifySynchronizationStarted()
        } else {
            UtilHelper().showView(check_in_progress, false)

            synchronizeStarted = false
            val snack = Snackbar.make(check_in_layout,
                    getString(R.string.error_no_internet_connection), Snackbar.LENGTH_LONG)
            UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
            itemStatus.isEnabled = true
        }
    }

    private fun addItemsToListView() {
        val checkIns = getCheckIns()

        if (checkIns.size != 0) {
            check_in_list_view.adapter = CheckInListAdapter(this, checkIns)
            check_in_text_no_check_ins.visibility = View.GONE
        }
    }

    private fun showCheckIns() {
        UtilHelper().showView(check_in_progress, true)

        setPointsOfSaleNumber()
        addItemsToListView()

        UtilHelper().showView(check_in_progress, false)
    }

    private fun goToScannerScreen() {
        val intent = Intent(this, ScannerActivity::class.java)
        startActivityForResult(intent, ScannerActivity.REQUEST_CODE_CAMERA)
        AnimationHelper().enterTransition(this)
    }

    private fun goToCheckInConfirm(route: String, pointOfSaleFolio: String) {
        val intent = CheckInConfirmActivity.getStartIntent(this, route, pointOfSaleFolio)
        startActivity(intent)
        AnimationHelper().enterTransition(this)
    }

    private fun setPointsOfSaleNumber() {
        val pointsOfSale = getPointsOfSale().size
        val checkIns = getCheckIns().size

        val doneCheckIns = "$checkIns/$pointsOfSale ${getString(R.string.check_in_done_check_ins)}"
        check_in_text_number.text = doneCheckIns
    }

    private fun setCodesToArray() {
        qrCode.add(0, "pos-000")
        qrCode.add(1, "pos-00")
        qrCode.add(2, "pos-0")
        qrCode.add(3, "pos-")
    }

    private fun validateCode(code: String): Boolean {
        var pointOfSaleCode = false

        for (qr in qrCode) {
            pointOfSaleCode = getPointsOfSale()
                    .map { "$qr${it.id}" }
                    .contains(code)
            if (pointOfSaleCode) {
                val index = code.indexOf('-')
                pointOfSaleFolio = if (index == -1) null.toString() else {
                    code.substring(index + 1)
                }
                break
            }
        }

        if (pointOfSaleCode) {
            route = RouteDao().findAll().first()?.code!!
            return true
        }
        return false
    }

    private fun tryToFindPointOSale(code: String) {
        val found = validateCode(code)

        if (found) {
            Log.d(tag, "-------> Point of sale was found!")
            goToCheckInConfirm(route, pointOfSaleFolio)
        } else {
            val snack = Snackbar.make(check_in_layout,
                    R.string.error_unregistered_pos,
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

    private fun setListenersToViews() {
        check_in_fab.setOnClickListener {
            if (StorageAccess().checkInternalStorageAvailable()) {
                validatePermissions()
            } else {
                UtilHelper().showStorageAvailableAlert(this)
            }
        }

        check_in_list_view.setOnScrollListener(object : RecyclerView.OnScrollListener(),
                AbsListView.OnScrollListener {
            override fun onScroll(p0: AbsListView?, p1: Int, p2: Int, p3: Int) {
                if (p1 == 0) {
                    check_in_fab.visibility = View.VISIBLE
                } else {
                    if (p1 != buttonPosition) {
                        check_in_fab.visibility = View.GONE
                        buttonPosition = p1
                    }
                }
            }

            override fun onScrollStateChanged(p0: AbsListView?, p1: Int) {
                check_in_fab.visibility = View.VISIBLE
            }
        })
    }

    override fun onCheckInSuccess() {
        setPointsOfSaleNumber()
        addItemsToListView()

        isSessionActive()
    }

    override fun onCheckInFailure(error: String) {
        Log.e(tag, "CheckIn failed: $error")
        UtilHelper().showView(check_in_progress, false)

        val snack = Snackbar.make(check_in_layout,
                getString(R.string.error_default), Snackbar.LENGTH_LONG)
        UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
        itemStatus.isEnabled = true
    }
}
