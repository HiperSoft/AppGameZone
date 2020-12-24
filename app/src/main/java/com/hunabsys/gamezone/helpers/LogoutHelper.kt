package com.hunabsys.gamezone.helpers

/* ktlint-disable no-wildcard-imports */
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.services.delegates.ILogoutDelegate
import com.hunabsys.gamezone.services.rest.HttpClientService
import com.hunabsys.gamezone.services.rest.LogoutService
import com.hunabsys.gamezone.views.activities.login.LoginActivity

class LogoutHelper : ILogoutDelegate {

    private var alertDialog: AlertDialog? = null
    private lateinit var appCompatActivity: AppCompatActivity

    fun tryToLogout(activity: AppCompatActivity) {
        appCompatActivity = activity
        val message: Int

        when {
            HttpClientService.sessionUnauthorized ->
                message = R.string.message_session_expired

            HttpClientService.userRoutesUnsigned ->
                message = R.string.error_user_unsigned_routes

            HttpClientService.userPointOfSaleUnsigned ->
                message = R.string.error_user_unsigned_pointofsales

            else -> {
                message = R.string.message_logout
                alertDialog = AlertDialog.Builder(activity)
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.action_logout) { dialog, which ->
                            if (isNetworkActive()) {
                                LogoutService(this, activity).logout()
                            } else {
                                onLogoutFailure("")
                            }
                        }
                        .setNegativeButton(R.string.action_cancel) { dialog, which -> dialog.dismiss() }.show()
                return
            }
        }

        alertDialog = AlertDialog.Builder(activity)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.action_logout) { dialog, which ->
                    if (isNetworkActive()) {
                        LogoutService(this, activity).logout()
                    } else {
                        onLogoutFailure("")
                    }
                }.show()
    }

    private fun logout() {
        val intent = Intent(appCompatActivity, LoginActivity::class.java)
        appCompatActivity.startActivity(intent)
        appCompatActivity.finish()
        AnimationHelper().exitTransition(this.appCompatActivity)

        PreferencesHelper(appCompatActivity).dropPreferences()
    }

    private fun isNetworkActive(): Boolean {
        val connectivityManager =
                appCompatActivity.getSystemService(Context.CONNECTIVITY_SERVICE)
                        as ConnectivityManager
        val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    override fun onLogoutSuccess() {
        Log.e("Session:", "Success")
        logout()
    }

    override fun onLogoutFailure(error: String) {
        Log.e("Session:", "Failure")
        logout()
    }
}