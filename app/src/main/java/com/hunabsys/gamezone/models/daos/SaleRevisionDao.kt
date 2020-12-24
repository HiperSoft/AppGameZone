package com.hunabsys.gamezone.models.daos

import android.util.Log
import com.hunabsys.gamezone.models.datamodels.SaleRevision
import com.rollbar.android.Rollbar
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort

/**
 * Data access object for SaleRevision.
 * Created by Silvia Valdez on 2/13/18.
 */
class SaleRevisionDao {

    private val tag = SaleRevisionDao::class.simpleName
    private val realm = Realm.getDefaultInstance()

    fun create(saleRevision: SaleRevision) {
        try {
            realm.executeTransaction {
                val saleRevisionObject = realm.createObject(SaleRevision::class.java, getId())
                saleRevisionObject.webId = saleRevision.webId
                saleRevisionObject.userId = saleRevision.userId
                saleRevisionObject.routeId = saleRevision.routeId
                saleRevisionObject.pointOfSaleId = saleRevision.pointOfSaleId
                saleRevisionObject.gameMachineId = saleRevision.gameMachineId

                saleRevisionObject.entry = saleRevision.entry
                saleRevisionObject.outcome = saleRevision.outcome
                saleRevisionObject.screen = saleRevision.screen
                saleRevisionObject.gameMachineOutcome = saleRevision.gameMachineOutcome
                saleRevisionObject.commissionAmount = saleRevision.commissionAmount
                saleRevisionObject.commissionPercentage = saleRevision.commissionPercentage
                saleRevisionObject.currentFund = saleRevision.currentFund
                saleRevisionObject.latitude = saleRevision.latitude
                saleRevisionObject.longitude = saleRevision.longitude
                saleRevisionObject.comments = saleRevision.comments

                saleRevisionObject.week = saleRevision.week
                saleRevisionObject.createdAt = saleRevision.createdAt

                saleRevisionObject.evidences = saleRevision.evidences

                saleRevisionObject.hasSynchronizedData = saleRevision.hasSynchronizedData
                saleRevisionObject.hasSynchronizedPhotos = saleRevision.hasSynchronizedPhotos
            }
        } catch (ex: IllegalArgumentException) {
            Log.e(tag, "Attempting to create a SaleRevision", ex)
            Rollbar.instance().critical(ex, tag)
        }
    }

    fun update(saleRevision: SaleRevision) {
        try {
            realm.executeTransaction {
                val saleRevisionObject = findById(saleRevision.id)
                saleRevisionObject.webId = saleRevision.webId
                saleRevisionObject.userId = saleRevision.userId
                saleRevisionObject.routeId = saleRevision.routeId
                saleRevisionObject.pointOfSaleId = saleRevision.pointOfSaleId
                saleRevisionObject.gameMachineId = saleRevision.gameMachineId

                saleRevisionObject.entry = saleRevision.entry
                saleRevisionObject.outcome = saleRevision.outcome
                saleRevisionObject.screen = saleRevision.screen
                saleRevisionObject.gameMachineOutcome = saleRevision.gameMachineOutcome
                saleRevisionObject.commissionAmount = saleRevision.commissionAmount
                saleRevisionObject.commissionPercentage = saleRevision.commissionPercentage
                saleRevisionObject.currentFund = saleRevision.currentFund
                saleRevisionObject.latitude = saleRevision.latitude
                saleRevisionObject.longitude = saleRevision.longitude
                saleRevisionObject.comments = saleRevision.comments

                saleRevisionObject.week = saleRevision.week
                saleRevisionObject.createdAt = saleRevision.createdAt

                saleRevisionObject.hasSynchronizedData = saleRevision.hasSynchronizedData
                saleRevisionObject.hasSynchronizedPhotos = saleRevision.hasSynchronizedPhotos
            }
        } catch (ex: IllegalArgumentException) {
            Log.e(tag, "Attempting to update a SaleRevision", ex)
            Rollbar.instance().critical(ex, tag)
        }
    }

    fun findCopyById(id: Long): SaleRevision {
        val saleRevision = realm.where(SaleRevision::class.java)
                .equalTo("id", id)
                .findFirst()!!
        return realm.copyFromRealm(saleRevision)
    }

    private fun findAll(): RealmResults<SaleRevision> {
        return realm.where(SaleRevision::class.java)
                .findAll()
                .sort("id", Sort.DESCENDING)
    }

    fun findAll(userId: Long): RealmResults<SaleRevision> {
        return realm.where(SaleRevision::class.java)
                .equalTo("userId", userId)
                .findAll()
                .sort("id", Sort.DESCENDING)
    }

    fun deleteAll(userId: Long) {
        try {
            realm.executeTransaction {
                realm.where(SaleRevision::class.java)
                        .equalTo("userId", userId)
                        .findAll()
                        .deleteAllFromRealm()
            }
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to delete all SaleRevisions", ex)
            Rollbar.instance().critical(ex, tag)
        }
    }

    fun deleteAllOtherUsers(userId: Long) {
        try {
            realm.executeTransaction {
                realm.where(SaleRevision::class.java)
                        .notEqualTo("userId", userId)
                        .findAll()
                        .deleteAllFromRealm()
            }
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to delete all SaleRevisions", ex)
            Rollbar.instance().critical(ex, tag)
        }
    }

    private fun findById(id: Long): SaleRevision {
        return realm.where(SaleRevision::class.java)
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