package com.hunabsys.gamezone.services.mappers

import android.util.Log
import com.hunabsys.gamezone.helpers.JsonValidationHelper
import com.hunabsys.gamezone.models.daos.EvidenceDao
import com.hunabsys.gamezone.models.daos.PrizeDao
import com.hunabsys.gamezone.models.datamodels.Evidence
import com.hunabsys.gamezone.models.datamodels.Prize
import io.realm.RealmList
import org.json.JSONArray
import org.json.JSONObject

class PrizeMapper {

    private val tag = PrizeMapper::class.simpleName

    companion object {
        var currentWebId: Long = 0
    }

    fun mapPrizes(prizes: JSONArray): Boolean {
        var result = true
        var success: Boolean
        var prize: JSONObject

        for (i in 0 until prizes.length()) {
            prize = prizes.getJSONObject(i)
            success = mapPrize(prize)

            if (!success) {
                result = false
            }
        }
        return result
    }

    private fun mapPrize(prize: JSONObject): Boolean {
        val id = JsonValidationHelper().getLongValue(prize, "mobile_id")
        val webId = JsonValidationHelper().getLongValue(prize, "web_id")
        val success = JsonValidationHelper().getBooleanValue(prize, "success")
        val error = JsonValidationHelper().getStringValue(prize, "error_message")

        if (success) {
            val prizeObject = PrizeDao().findCopyById(id)
            updatePrize(prizeObject, webId)

            val evidences = prizeObject.evidences
            updateEvidences(evidences, webId)

            return true
        } else {
            if (error != "") {
                Log.e(tag, error)
            }
        }
        return false
    }

    private fun updatePrize(prize: Prize, webId: Long) {
        prize.webId = webId
        PrizeDao().update(prize)

        val updatedPrize = PrizeDao().findCopyById(prize.id)
        Log.e(tag, "-------> Updated Prize with ID: " + updatedPrize.id
                + ", synced: " + updatedPrize.hasSynchronizedData)
    }

    private fun updateEvidences(evidences: RealmList<Evidence>, webId: Long) {
        for (evidence in evidences) {
            evidence.evidenceableId = webId

            EvidenceDao().update(evidence)

            currentWebId = webId

            Log.e(tag, "-------> Updated Evidences for Prize with webID: $webId")
        }
    }
}