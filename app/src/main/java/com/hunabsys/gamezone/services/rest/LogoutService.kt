package com.hunabsys.gamezone.services.rest

import android.content.Context
import android.util.Log
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.services.delegates.IHttpClientDelegate
import com.hunabsys.gamezone.services.delegates.ILogoutDelegate
import com.hunabsys.gamezone.services.mappers.LogoutMapper
import com.rollbar.android.Rollbar
import org.json.JSONObject
import java.net.HttpURLConnection

class LogoutService(val delegate: ILogoutDelegate, val context: Context) : IHttpClientDelegate {

    private val tag = RouteConfigurationService::class.simpleName

    private val httpClientService = HttpClientService(context, this)
    private val url = "${httpClientService.getServer()}api/auth/sign_out"

    fun logout(): Boolean {
        return if (httpClientService.networkAvailable()) {
            httpClientService.delete(url)
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
                val mapped = LogoutMapper().mapLogout(response)

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
        delegate.onLogoutSuccess()
    }

    private fun notifyError(error: String) {
        delegate.onLogoutFailure(error)
    }
}