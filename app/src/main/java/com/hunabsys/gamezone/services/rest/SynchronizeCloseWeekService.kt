package com.hunabsys.gamezone.services.rest

import android.content.Context
import android.util.Log
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.models.daos.RouteDao
import com.hunabsys.gamezone.services.delegates.ICloseWeekDelegate
import com.hunabsys.gamezone.services.delegates.IHttpClientDelegate
import com.hunabsys.gamezone.services.mappers.CloseWeekMapper
import com.rollbar.android.Rollbar
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection

class SynchronizeCloseWeekService(val context: Context) : IHttpClientDelegate {

    private val tag = SynchronizeCloseWeekService::class.simpleName

    private val httpClientService = HttpClientService(context, this)
    private val url = "${httpClientService.getServer()}api/routes/${getRouteId()}/close_week.json"

    fun synchronizeCloseWeek(data: JSONObject): Boolean {
        return if (httpClientService.networkAvailable()) {
            httpClientService.post(url, data.toString())
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
                val success = CloseWeekMapper().mapCloseWeeks(response)
                if (success) {
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
        val delegate = context as ICloseWeekDelegate
        delegate.onCloseWeekSuccess()
    }

    private fun notifyError(error: String) {
        val delegate = context as ICloseWeekDelegate
        delegate.onCloseWeekFailure(error)
    }

    private fun getRouteId(): String {
        return RouteDao().findAll().first()?.id.toString()
    }
}
