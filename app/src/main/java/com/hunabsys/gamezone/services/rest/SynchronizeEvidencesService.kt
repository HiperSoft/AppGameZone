package com.hunabsys.gamezone.services.rest

import android.content.Context
import android.util.Log
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.services.delegates.IEvidenceDelegate
import com.hunabsys.gamezone.services.delegates.IHttpClientDelegate
import com.hunabsys.gamezone.services.mappers.EvidencesMapper
import com.rollbar.android.Rollbar
import org.json.JSONObject
import java.net.HttpURLConnection

class SynchronizeEvidencesService(val context: Context) : IHttpClientDelegate {

    private val tag = SynchronizeEvidencesService::class.simpleName

    private val httpClientService = HttpClientService(context, this)
    private val url = "${httpClientService.getServer()}api/evidences/base64"

    var evidenceId: Long = 0

    fun synchronizeEvidences(data: JSONObject, id: Long): Boolean {
        return if (httpClientService.networkAvailable()) {
            httpClientService.post(url, data.toString())
            evidenceId = id
            true
        } else {
            false
        }
    }

    override fun onSuccess(result: ArrayList<Any>) {
        val defaultError = context.getString(R.string.error_default)

        if (result[0] == HttpURLConnection.HTTP_CREATED) {
            try {
                val response = JSONObject(result[1].toString())
                validateResponse(response, evidenceId)
                return
            } catch (ex: Exception) {
                Log.e(tag, "Attempting to validate response", ex)
                Rollbar.instance().error(ex, tag)
            }
        }
        notifyError(defaultError)
    }

    private fun validateResponse(data: JSONObject, evidenceId: Long) {
        val success = EvidencesMapper().mapEvidence(data, evidenceId)
        if (success) {
            notifySuccess()
        } else {
            val error = context.getString(R.string.evidences_some_failed)
            notifyError(error)
        }
    }

    override fun onFailure(error: String) {
        notifyError(error)
    }

    private fun notifySuccess() {
        val delegate = context as IEvidenceDelegate
        delegate.onEvidenceSuccess()
    }

    private fun notifyError(error: String) {
        val delegate = context as IEvidenceDelegate
        delegate.onEvidenceFailure(error)
    }
}