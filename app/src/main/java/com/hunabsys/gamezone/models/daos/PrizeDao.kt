package com.hunabsys.gamezone.models.daos

import android.util.Log
import com.hunabsys.gamezone.models.datamodels.Prize
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort

/**
 * Prize model
 * Created by Jonathan Hernandez on 23/07/2018
 */
class PrizeDao {

    private val tag = PrizeDao::class.simpleName
    private val realm = Realm.getDefaultInstance()

    fun create(prize: Prize) {
        try {
            realm.executeTransaction {
                val prizeObject = realm.createObject(Prize::class.java, getId())
                prizeObject.webId = prize.webId
                prizeObject.userId = prize.userId
                prizeObject.routeId = prize.routeId
                prizeObject.pointOfSaleId = prize.pointOfSaleId
                prizeObject.gameMachineId = prize.gameMachineId

                prizeObject.inputReading = prize.inputReading
                prizeObject.outputReading = prize.outputReading
                prizeObject.screen = prize.screen
                prizeObject.prizeAmount = prize.prizeAmount
                prizeObject.currentAmount = prize.currentAmount
                prizeObject.toComplete = prize.toComplete
                prizeObject.gameMachineFund = prize.gameMachineFund
                prizeObject.expenseAmount = prize.expenseAmount
                prizeObject.comments = prize.comments

                prizeObject.week = prize.week
                prizeObject.createdAt = prize.createdAt

                prizeObject.evidences = prize.evidences

                prizeObject.hasSynchronizedData = prize.hasSynchronizedData
                prizeObject.hasSynchronizedPhotos = prize.hasSynchronizedPhotos
            }
        } catch (ex: IllegalArgumentException) {
            Log.e(tag, "Attempting to create a Prize", ex)
        }
    }

    fun update(prize: Prize) {
        try {
            realm.executeTransaction {
                val prizeObject = findById(prize.id)
                prizeObject.webId = prize.webId
                prizeObject.userId = prize.userId
                prizeObject.routeId = prize.routeId
                prizeObject.pointOfSaleId = prize.pointOfSaleId
                prizeObject.gameMachineId = prize.gameMachineId

                prizeObject.inputReading = prize.inputReading
                prizeObject.outputReading = prize.outputReading
                prizeObject.screen = prize.screen
                prizeObject.prizeAmount = prize.prizeAmount
                prizeObject.currentAmount = prize.currentAmount
                prizeObject.toComplete = prize.toComplete
                prizeObject.gameMachineFund = prize.gameMachineFund
                prizeObject.expenseAmount = prize.expenseAmount
                prizeObject.comments = prize.comments

                prizeObject.week = prize.week
                prizeObject.createdAt = prize.createdAt

                prizeObject.hasSynchronizedData = prize.hasSynchronizedData
                prizeObject.hasSynchronizedPhotos = prize.hasSynchronizedPhotos
            }
        } catch (ex: IllegalArgumentException) {
            Log.e(tag, "Attempting to update a Prize", ex)
        }
    }

    private fun findById(id: Long): Prize {
        return realm.where(Prize::class.java)
                .equalTo("id", id)
                .findFirst()!!
    }

    fun findCopyById(id: Long): Prize {
        val prize = realm.where(Prize::class.java)
                .equalTo("id", id)
                .findFirst()!!
        return realm.copyFromRealm(prize)
    }

    private fun findAll(): RealmResults<Prize> {
        return realm.where(Prize::class.java)
                .findAll()
                .sort("id", Sort.DESCENDING)
    }

    fun findAll(userId: Long): RealmResults<Prize> {
        return realm.where(Prize::class.java)
                .equalTo("userId", userId)
                .findAll()
                .sort("id", Sort.DESCENDING)
    }

    fun deleteAll(userId: Long) {
        try {
            realm.executeTransaction {
                realm.where(Prize::class.java)
                        .equalTo("userId", userId)
                        .findAll()
                        .deleteAllFromRealm()
            }
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to delete all Prizes", ex)
        }
    }

    fun deleteAllOtherUsers(userId: Long) {
        try {
            realm.executeTransaction {
                realm.where(Prize::class.java)
                        .notEqualTo("userId", userId)
                        .findAll()
                        .deleteAllFromRealm()
            }
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to delete all Prizes", ex)
        }
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