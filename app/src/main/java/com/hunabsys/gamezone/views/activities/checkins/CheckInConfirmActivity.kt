package com.hunabsys.gamezone.views.activities.checkins

/* ktlint-disable no-wildcard-imports */
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.util.Log
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.AnimationHelper
import com.hunabsys.gamezone.helpers.GpsHelper
import com.hunabsys.gamezone.helpers.PreferencesHelper
import com.hunabsys.gamezone.helpers.UtilHelper
import com.hunabsys.gamezone.models.daos.CheckInDao
import com.hunabsys.gamezone.models.datamodels.CheckIn
import com.hunabsys.gamezone.views.activities.BaseActivity
import com.hunabsys.gamezone.views.activities.ScannerActivity
import com.hunabsys.gamezone.views.activities.checkins.ICheckInConstants.Companion.FINISH_DELAY
import com.hunabsys.gamezone.views.activities.checkins.ICheckInConstants.Companion.RESULT_CODE_SUCCESS
import com.rollbar.android.Rollbar
import kotlinx.android.synthetic.main.activity_check_in_confirm.*
import java.util.*

class CheckInConfirmActivity : BaseActivity() {

    private val tag = CheckInConfirmActivity::class.java.simpleName

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this activity using the provided parameters.
         *
         * @param context Previous activity context.
         * @param pointOfSaleFolio String containing point of sale folio.
         * @return A new intent for activity SaleRevisionSummaryActivity.
         */
        fun getStartIntent(context: Context, route: String, pointOfSaleFolio: String): Intent {
            val intent = Intent(context, CheckInConfirmActivity::class.java)
            intent.putExtra(ScannerActivity.EXTRA_RESULT_CODE, route)
            intent.putExtra(ICheckInConstants.EXTRA_MACHINE_FOLIO, pointOfSaleFolio)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_in_confirm)

        GpsHelper().startLocationService(tag, this)

        setListenersForSaveButton()
    }

    private fun getCode(): String {
        return this.intent.extras.getString(ICheckInConstants.EXTRA_MACHINE_FOLIO)
    }

    private fun setData(checkIn: CheckIn): Boolean {
        try {
            val pointOfSaleId = getCode()

            checkIn.userId = PreferencesHelper(this).userId
            checkIn.pointOfSaleId = pointOfSaleId.toLong()
            checkIn.createdAt = Date()
            checkIn.isSynchronized = false

            val currentLocation = GpsHelper.locationGps
            if (currentLocation != null) {
                checkIn.latitude = currentLocation.latitude
                checkIn.longitude = currentLocation.longitude
            }

            Log.e(tag, "Latitude: ${checkIn.latitude}, Logitude: ${checkIn.longitude}")
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to save ID's and other data to check in object", ex)
            Rollbar.instance().error(ex, tag)
            return false
        }
        return true
    }

    private fun saveCheckIn(): Boolean {
        val checkIn = CheckIn()
        val savedData = setData(checkIn)

        if (savedData) {
            CheckInDao().create(checkIn)
            Log.d(tag, "created new check in")
        } else {
            return false
        }
        return true
    }

    private fun showProgress(show: Boolean) {
        UtilHelper().showView(check_in_save_progress, show)
        check_in_button_save.isEnabled = !show
    }

    private fun setListenersForSaveButton() {
        check_in_button_save.setOnClickListener {
            showProgress(true)
            val saved = saveCheckIn()

            if (saved) {
                val snack = Snackbar.make(check_in_confirmation_layout,
                        R.string.message_saved_changes, Snackbar.LENGTH_LONG)
                UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_SUCCESS)

                setResult(RESULT_CODE_SUCCESS)

                Handler().postDelayed({
                    showProgress(false)
                    finish()
                    AnimationHelper().exitTransition(this)
                }, FINISH_DELAY)
            } else {
                showProgress(false)
                val snack = Snackbar.make(check_in_confirmation_layout, R.string.error_saving,
                        Snackbar.LENGTH_LONG)
                UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
            }
        }
    }
}