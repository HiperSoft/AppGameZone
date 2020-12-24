package com.hunabsys.gamezone.services.rest

/* ktlint-disable no-wildcard-imports */
import android.content.Context
import android.util.Log
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.PreferencesHelper
import com.hunabsys.gamezone.helpers.UtilHelper
import com.hunabsys.gamezone.services.delegates.IHttpClientDelegate
import com.hunabsys.gamezone.services.delegates.IRouteConfigurationDelegate
import com.hunabsys.gamezone.services.mappers.RouteMapper
import org.json.JSONArray
import java.net.HttpURLConnection
import java.util.*

class RouteConfigurationService(val context: Context) : IHttpClientDelegate {

    private val tag = RouteConfigurationService::class.simpleName

    private val httpClientService = HttpClientService(context, this)
    private val url = "${httpClientService.getServer()}api/routes_configuration"

    fun getConfiguration(): Boolean {
        return if (httpClientService.networkAvailable()) {
            httpClientService.get(url)
            true
        } else {
            false
        }
    }

    override fun onSuccess(result: ArrayList<Any>) {
        val defaultError = context.getString(R.string.error_default)

        if (result[0] == HttpURLConnection.HTTP_OK) {
            try {
                val response = JSONArray(result[1].toString())
                saveConfiguration(response)
                notifySuccess()
                return
            } catch (ex: Exception) {
                Log.d(tag, "Attempting to validate response", ex)
                UtilHelper().searchError(ex)
            }
        }
        notifyError(defaultError)
    }

    override fun onFailure(error: String) {
        notifyError(error)
    }

    private fun saveConfiguration(data: JSONArray) {
        RouteMapper().mapRoute(data)

        PreferencesHelper(context).savedConfig = true
        Log.d(tag, "------- New configuration is saved!")
    }

    private fun notifySuccess() {
        val delegate = context as IRouteConfigurationDelegate
        delegate.onRouteConfigurationSuccess()
    }

    private fun notifyError(error: String) {
        val delegate = context as IRouteConfigurationDelegate
        delegate.onRouteConfigurationFailure(error)
    }
}