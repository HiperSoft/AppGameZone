package com.hunabsys.gamezone.services.mappers

import android.util.Log
import com.hunabsys.gamezone.helpers.JsonValidationHelper
import com.hunabsys.gamezone.models.daos.EvidenceDao
import com.hunabsys.gamezone.models.daos.SaleRevisionDao
import com.hunabsys.gamezone.models.datamodels.Evidence
import com.hunabsys.gamezone.models.datamodels.SaleRevision
import io.realm.RealmList
import org.json.JSONArray
import org.json.JSONObject

class SaleRevisionMapper {

    private val tag = SaleRevisionMapper::class.simpleName

    companion object {
        var currentWebId: Long = 0
    }

    fun mapRevisions(saleRevisions: JSONArray): Boolean {
        var result = true
        var success: Boolean
        var saleRevision: JSONObject

        for (i in 0 until saleRevisions.length()) {
            saleRevision = saleRevisions.getJSONObject(i)
            success = mapSaleRevision(saleRevision)

            if (!success) {
                result = false
            }
        }
        return result
    }

    private fun mapSaleRevision(saleRevision: JSONObject): Boolean {
        val id = JsonValidationHelper().getLongValue(saleRevision, "mobile_id")
        val webId = JsonValidationHelper().getLongValue(saleRevision, "web_id")
        val success = JsonValidationHelper().getBooleanValue(saleRevision, "success")
        val error = JsonValidationHelper().getStringValue(saleRevision, "error_message")

        if (success) {
            val saleRevisionObject = SaleRevisionDao().findCopyById(id)
            updateSaleRevision(saleRevisionObject, webId)

            val evidences = saleRevisionObject.evidences
            updateEvidences(evidences, webId)

            return true
        } else {
            if (error != "") {
                Log.e(tag, error)
            }
        }
        return false
    }

    private fun updateSaleRevision(saleRevision: SaleRevision, webId: Long) {
        saleRevision.webId = webId
        SaleRevisionDao().update(saleRevision)

        val updatedSaleRevision = SaleRevisionDao().findCopyById(saleRevision.id)
        Log.e(tag, "-------> Updated SaleRevision with ID: " + updatedSaleRevision.id
                + ", synced: " + updatedSaleRevision.hasSynchronizedData)
    }

    private fun updateEvidences(evidences: RealmList<Evidence>, webId: Long) {
        for (evidence in evidences) {
            evidence.evidenceableId = webId

            EvidenceDao().update(evidence)

            currentWebId = webId

            Log.e(tag, "-------> Updated Evidences for SaleRevision with webID: $webId")
        }
    }
}