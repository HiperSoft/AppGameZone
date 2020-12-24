package com.hunabsys.gamezone.models.daos

import android.util.Log
import com.hunabsys.gamezone.models.datamodels.Evidence
import com.rollbar.android.Rollbar
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort

class EvidenceDao {

    private val tag = EvidenceDao::class.java.simpleName
    private val realm = Realm.getDefaultInstance()

    fun create(evidence: Evidence) {
        try {
            realm.executeTransaction {
                val evidenceObject = realm.createObject(Evidence::class.java, getId())
                evidenceObject.userId = evidence.userId
                evidenceObject.evidenceId = evidence.evidenceId
                evidenceObject.evidenceableId = evidence.evidenceableId

                evidenceObject.evidenceableType = evidence.evidenceableType
                evidenceObject.file = evidence.file
                evidenceObject.filename = evidence.filename
                evidenceObject.originalFilename = evidence.originalFilename
                evidenceObject.isSynchronized = evidence.isSynchronized
            }
        } catch (ex: IllegalArgumentException) {
            Log.e(tag, "Attempting to create an Evidence", ex)
            Rollbar.instance().critical(ex, tag)
        }
    }

    fun update(evidence: Evidence) {
        try {
            realm.executeTransaction {
                val evidenceObject = findById(evidence.id)
                evidenceObject.userId = evidence.userId
                evidenceObject.evidenceId = evidence.evidenceId
                evidenceObject.evidenceableId = evidence.evidenceableId

                evidenceObject.evidenceableType = evidence.evidenceableType
                evidenceObject.file = evidence.file
                evidenceObject.filename = evidence.filename
                evidenceObject.originalFilename = evidence.originalFilename
                evidenceObject.isSynchronized = evidence.isSynchronized
            }
        } catch (ex: IllegalArgumentException) {
            Log.e(tag, " Attempting to update an Evidence")
            Rollbar.instance().critical(ex, tag)
        }
    }

    private fun findById(id: Long): Evidence {
        return realm.where(Evidence::class.java)
                .equalTo("id", id)
                .findFirst()!!
    }

    fun findCopyById(id: Long): Evidence {
        val evidence = realm.where(Evidence::class.java)
                .equalTo("id", id)
                .findFirst()!!
        return realm.copyFromRealm(evidence)
    }

    fun findAllByType(type: String, userId: Long): RealmResults<Evidence> {
        return realm.where(Evidence::class.java)
                .equalTo("userId", userId)
                .equalTo("evidenceableType", type)
                .findAll()
                .sort("id", Sort.DESCENDING)
    }

    private fun findAll(): RealmResults<Evidence> {
        return realm.where(Evidence::class.java)
                .findAll()
                .sort("id", Sort.DESCENDING)
    }

    fun deleteAll(userId: Long) {
        try {
            realm.executeTransaction {
                realm.where(Evidence::class.java)
                        .equalTo("userId", userId)
                        .findAll()
                        .deleteAllFromRealm()
            }
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to delete all Evidences", ex)
            Rollbar.instance().critical(ex, tag)
        }
    }

    fun deleteAllOtherUsers(userId: Long) {
        try {
            realm.executeTransaction {
                realm.where(Evidence::class.java)
                        .notEqualTo("userId", userId)
                        .findAll()
                        .deleteAllFromRealm()
            }
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to delete all Evidences", ex)
            Rollbar.instance().critical(ex, tag)
        }
    }

    fun getId(): Long {
        return if (findAll().size != 0) {
            val lastId = findAll().first()!!.id
            lastId + 1
        } else {
            1
        }
    }

    fun saveEvidence(evidence: Evidence): Evidence? {
        var savedEvidence: Evidence? = null
        create(evidence)

        try {
            savedEvidence = findById(evidence.id)
            Log.d(tag, "Evidence is saved: " + savedEvidence.toString())
        } catch (ex: KotlinNullPointerException) {
            Log.e(tag, "Null Evidence", ex)
            Rollbar.instance().critical(ex, tag)
        }

        return savedEvidence
    }
}