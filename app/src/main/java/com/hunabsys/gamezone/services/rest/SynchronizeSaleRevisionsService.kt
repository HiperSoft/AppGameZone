package com.hunabsys.gamezone.services.rest

import android.content.Context
import android.util.Log
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.services.delegates.IHttpClientDelegate
import com.hunabsys.gamezone.services.delegates.ISaleRevisionDelegate
import com.hunabsys.gamezone.services.mappers.SaleRevisionMapper
import com.rollbar.android.Rollbar
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection

class SynchronizeSaleRevisionsService(val context: Context) : IHttpClientDelegate {

    private val tag = SynchronizeSaleRevisionsService::class.simpleName

    private val httpClientService = HttpClientService(context, this)
    private val url = "${httpClientService.getServer()}api/sale_revisions/save_array.json"

    fun synchronizeRevisions(data: JSONObject): Boolean {
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
                validateResponse(response)
                return
            } catch (ex: Exception) {
                Log.e(tag, "Attempting to validate response", ex)
                Rollbar.instance().error(ex, tag)
            }
        }
        notifyError(defaultError)
    }

    private fun validateResponse(data: JSONArray) {
        val success = SaleRevisionMapper().mapRevisions(data)
        if (success) {
            notifySuccess()
        } else {
            val error = context.getString(R.string.sale_revisions_some_failed)
            notifyError(error)
        }
    }

    override fun onFailure(error: String) {
        notifyError(error)
    }

    private fun notifySuccess() {
        val delegate = context as ISaleRevisionDelegate
        delegate.onSaleRevisionSuccess(SaleRevisionMapper.currentWebId)
    }

    private fun notifyError(error: String) {
        val delegate = context as ISaleRevisionDelegate
        delegate.onSaleRevisionFailure(error)
    }
}