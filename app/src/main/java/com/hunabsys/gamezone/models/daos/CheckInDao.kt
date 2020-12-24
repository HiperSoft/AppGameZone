package com.hunabsys.gamezone.models.daos

import android.util.Log
import com.hunabsys.gamezone.models.datamodels.CheckIn
import com.rollbar.android.Rollbar
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort

class CheckInDao {

    private val tag = CheckInDao::class.java.simpleName
    private val realm = Realm.getDefaultInstance()

    fun create(checkIn: CheckIn) {
        try {
            realm.executeTransaction {
                val checkInObject = realm.createObject(CheckIn::class.java, getId())
                checkInObject.userId = checkIn.userId
                checkInObject.pointOfSaleId = checkIn.pointOfSaleId
                checkInObject.latitude = checkIn.latitude
                checkInObject.longitude = checkIn.longitude

                checkInObject.isSynchronized = checkIn.isSynchronized
                checkInObject.createdAt = checkIn.createdAt
            }
        } catch (ex: IllegalArgumentException) {
            Log.e(tag, "Attempting to create a CheckIn", ex)
            Rollbar.instance().critical(ex, tag)
        }
    }

    fun update(checkIn: CheckIn) {
        try {
            realm.executeTransaction {
                val checkInObject = findById(checkIn.id)
                checkInObject.userId = checkIn.userId
                checkInObject.pointOfSaleId = checkIn.pointOfSaleId
                checkInObject.latitude = checkIn.latitude
                checkInObject.longitude = checkIn.longitude
                checkInObject.createdAt = checkIn.createdAt
                checkInObject.isSynchronized = checkIn.isSynchronized
            }
        } catch (ex: IllegalArgumentException) {
            Log.e(tag, " Attempting to update a CheckIn")
            Rollbar.instance().critical(ex, tag)
        }
    }

    fun findCopyById(id: Long): CheckIn {
        val checkIn = realm.where(CheckIn::class.java)
                .equalTo("id", id)
                .findFirst()!!
        return realm.copyFromRealm(checkIn)
    }

    private fun findAll(): RealmResults<CheckIn> {
        return realm.where(CheckIn::class.java)
                .findAll()
                .sort("id", Sort.DESCENDING)
    }

    fun findAll(userId: Long): RealmResults<CheckIn> {
        return realm.where(CheckIn::class.java)
                .equalTo("userId", userId)
                .findAll()
                .sort("id", Sort.DESCENDING)
    }

    fun deleteAll(userId: Long) {
        try {
            realm.executeTransaction {
                realm.where(CheckIn::class.java)
                        .equalTo("userId", userId)
                        .findAll()
                        .deleteAllFromRealm()
            }
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to delete all CheckIns", ex)
            Rollbar.instance().critical(ex, tag)
        }
    }

    fun deleteAllOtherUsers(userId: Long) {
        try {
            realm.executeTransaction {
                realm.where(CheckIn::class.java)
                        .notEqualTo("userId", userId)
                        .findAll()
                        .deleteAllFromRealm()
            }
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to delete all CheckIns", ex)
            Rollbar.instance().critical(ex, tag)
        }
    }

    private fun findById(id: Long): CheckIn {
        return realm.where(CheckIn::class.java)
                .equalTo("id", id)
                .findFirst()!!
    }

    private fun getId(): Long {
        return if (findAll().size != 0) {
            val lastId = findAll().first()!!.id
            lastId + 1
        } else {
            1
        }
    }
}