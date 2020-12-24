package com.hunabsys.gamezone.services.mappers

import android.util.Log
import com.hunabsys.gamezone.helpers.JsonValidationHelper
import com.hunabsys.gamezone.models.daos.EvidenceDao
import com.hunabsys.gamezone.models.datamodels.Evidence
import org.json.JSONObject

class EvidencesMapper {

    private val tag = EvidencesMapper::class.simpleName

    fun mapEvidence(evidence: JSONObject, evidenceId: Long): Boolean {
        val id = JsonValidationHelper().getLongValue(evidence, "id")

        if (id != 0L) {
            val evidenceObject = EvidenceDao().findCopyById(evidenceId)
            updateEvidence(evidenceObject, evidenceId)
            return true
        }
        return false
    }

    private fun updateEvidence(evidenceObject: Evidence, evidenceId: Long) {
        evidenceObject.isSynchronized = true

        EvidenceDao().update(evidenceObject)

        val updateEvidences = EvidenceDao().findCopyById(evidenceId)
        Log.e(tag, "Evidences updated with ID " + evidenceObject.id + ", synced: "
                + updateEvidences.isSynchronized)
    }
}