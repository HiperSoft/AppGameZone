package com.hunabsys.gamezone.views.activities.login

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.telephony.TelephonyManager
import android.util.Log
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.AnimationHelper
import com.hunabsys.gamezone.helpers.PermissionsHelper
import com.hunabsys.gamezone.helpers.UtilHelper
import com.hunabsys.gamezone.services.delegates.ILoginDelegate
import com.hunabsys.gamezone.services.rest.LoginService
import com.hunabsys.gamezone.views.activities.MainActivity
import com.rollbar.android.Rollbar
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONObject

const val USER_KEY = "login"
const val PASSWORD_KEY = "password"
const val IMEI_KEY = "imei"
const val MIN_LENGTH = 6

class LoginActivity : AppCompatActivity(), ILoginDelegate {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        Rollbar.init(this)

        setListenerToLoginButton()
    }

    override fun onBackPressed() {
        finish()
        AnimationHelper().exitTransition(this)
        super.onBackPressed()
    }

    override fun onLoginSuccess() {
        showProgress(false)
        goToDashboard()
    }

    override fun onLoginFailure(error: String) {
        showProgress(false)

        val snack = Snackbar.make(login_layout_container, error, Snackbar.LENGTH_LONG)
        UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            PermissionsHelper.REQUEST_PERMISSIONS ->
                // If request is cancelled, the result arrays are empty.
                if (PermissionsHelper(this).validatePermissionResult(grantResults)) {
                    tryToLogin()
                } else {
                    val snack = Snackbar.make(login_layout_container,
                            R.string.error_missing_permissions,
                            Snackbar.LENGTH_LONG)
                    UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_WARNING)
                }
        }
    }

    private fun validatePermission() {
        val hasPermissions = PermissionsHelper(this).requestPhonePermissions()

        if (hasPermissions) {
            tryToLogin()
        }
    }

    @SuppressLint("MissingPermission")
    private fun tryToLogin() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val imei = telephonyManager.deviceId
        Log.e("IMEI", imei)

        showProgress(true)

        val userName = login_auto_user_name.text.toString().trim()
        val password = login_edit_password.text.toString().trim()

        val validCredentials = validateCredentials(userName, password)

        if (validCredentials) {
            val credentials = getFormattedCredentials(userName, password, imei)
            attemptLogin(credentials)
        } else {
            showProgress(false)
        }
    }

    private fun setListenerToLoginButton() {
        login_button_login.setOnClickListener {
            validatePermission()
        }
    }

    private fun enableViews(enabled: Boolean) {
        login_auto_user_name.isEnabled = enabled
        login_edit_password.isEnabled = enabled
        login_button_login.isEnabled = enabled
    }

    private fun showProgress(show: Boolean) {
        UtilHelper().showView(login_progress, show)
        enableViews(!show)
    }

    private fun validateCredentials(userName: String, password: String): Boolean {
        return validateUserName(userName) && validatePassword(password)
    }

    private fun validateUserName(userName: String): Boolean {
        val regex = Regex("^[a-zA-Z0-9]*$")
        val snack: Snackbar

        when {
            userName.isEmpty() -> {
                snack = Snackbar.make(login_layout_container,
                        R.string.error_empty_username,
                        Snackbar.LENGTH_SHORT)
                UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_WARNING)
                return false
            }
            userName.length < MIN_LENGTH -> {
                snack = Snackbar.make(login_layout_container,
                        R.string.error_unauthorized,
                        Snackbar.LENGTH_SHORT)
                UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
                return false
            }
            userName.contains("@") -> {
                snack = Snackbar.make(login_layout_container,
                        R.string.error_wrong_user_name,
                        Snackbar.LENGTH_LONG)
                UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_WARNING)
                return false
            }
            !userName.matches(regex = regex) -> {
                snack = Snackbar.make(login_layout_container,
                        R.string.error_wrong_user_name,
                        Snackbar.LENGTH_LONG)
                UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_WARNING)
                return false
            }
        }
        return true
    }

    private fun validatePassword(pass: String): Boolean {
        val snack: Snackbar
        when {
            pass.isEmpty() -> {
                snack = Snackbar.make(login_layout_container,
                        R.string.error_empty_password,
                        Snackbar.LENGTH_SHORT)
                UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_WARNING)
                return false
            }
            pass.length < MIN_LENGTH -> {
                snack = Snackbar.make(login_layout_container,
                        R.string.error_unauthorized,
                        Snackbar.LENGTH_SHORT)
                UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
                return false
            }
        }
        return true
    }

    private fun getFormattedCredentials(userName: String, password: String, imei: String): String {
        return JSONObject()
                .put(USER_KEY, userName)
                .put(PASSWORD_KEY, password)
                .put(IMEI_KEY, imei)
                .toString()
    }

    private fun attemptLogin(credentials: String) {
        val signingIn = LoginService(this).signIn(credentials)

        if (!signingIn) {
            showProgress(false)

            val snack = Snackbar.make(login_layout_container,
                    R.string.error_no_internet_connection,
                    Snackbar.LENGTH_LONG)
            UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
        }
    }

    private fun goToDashboard() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
        AnimationHelper().enterTransition(this)
    }
}
