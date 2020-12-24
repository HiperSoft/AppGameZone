package com.hunabsys.gamezone.services.rest
/* ktlint-disable no-wildcard-imports */
import android.content.Context
import android.util.Log
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.services.delegates.IHttpClientDelegate
import com.hunabsys.gamezone.services.delegates.ILoginDelegate
import com.hunabsys.gamezone.services.mappers.UserMapper
import com.rollbar.android.Rollbar
import org.json.JSONObject
import java.net.HttpURLConnection

class LoginService(val context: Context) : IHttpClientDelegate {

    private val tag = RouteConfigurationService::class.simpleName

    private val httpClientService = HttpClientService(context, this)
    private val url = "${httpClientService.getServer()}api/auth/sign_in"

    fun signIn(credentials: String): Boolean {
        return if (httpClientService.networkAvailable()) {
            httpClientService.post(url, credentials)
            true
        } else {
            false
        }
    }

    override fun onSuccess(result: ArrayList<Any>) {
        val defaultError = context.getString(R.string.error_default)

        if (result[0] == HttpURLConnection.HTTP_OK) {
            try {
                val response = JSONObject(result[1].toString())
                val mapped = UserMapper().mapUser(context, response)

                if (mapped) {
                    notifySuccess()
                    return
                }
            } catch (ex: Exception) {
                Log.e(tag, "Attempting to validate response", ex)
                Rollbar.instance().error(ex, tag)
            }
        }
        notifyError(defaultError)
    }

    override fun onFailure(error: String) {
        notifyError(error)
    }

    private fun notifySuccess() {
        val delegate = context as ILoginDelegate
        delegate.onLoginSuccess()
        HttpClientService.sessionUnauthorized = false
        HttpClientService.userRoutesUnsigned = false
        HttpClientService.userPointOfSaleUnsigned = false
    }

    private fun notifyError(error: String) {
        val delegate = context as ILoginDelegate
        delegate.onLoginFailure(error)
    }
}