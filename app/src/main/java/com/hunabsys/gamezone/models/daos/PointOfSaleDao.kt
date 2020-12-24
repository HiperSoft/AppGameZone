package com.hunabsys.gamezone.models.daos

import android.util.Log
import com.hunabsys.gamezone.models.datamodels.PointOfSale
import com.rollbar.android.Rollbar
import io.realm.Realm
import io.realm.RealmResults

/**
 * Data access object for PointOfSale.
 * Created by Silvia Valdez on 11/02/2018.
 */
class PointOfSaleDao {

    private val tag = PointOfSaleDao::class.simpleName
    private val realm = Realm.getDefaultInstance()

    private fun create(pointOfSale: PointOfSale) {
        try {
            realm.executeTransaction {
                val posObject = realm.createObject(PointOfSale::class.java, pointOfSale.id)
                posObject.name = pointOfSale.name
                posObject.commissionPercentage = pointOfSale.commissionPercentage
                posObject.gameMachines = pointOfSale.gameMachines
            }
        } catch (ex: IllegalArgumentException) {
            Log.e(tag, "Attempting to create a PointOfSale", ex)
            Rollbar.instance().critical(ex, tag)
        }
    }

    fun findAll(): RealmResults<PointOfSale> {
        return realm.where(PointOfSale::class.java).findAll()!!
    }

    fun findById(id: Long): PointOfSale? {
        val pointsOfSale = realm.where(PointOfSale::class.java).equalTo("id", id)
        if (pointsOfSale != null) {
            return pointsOfSale.findFirst()
        }
        return null
    }

    fun deleteAll() {
        try {
            realm.executeTransaction {
                val pointsOfSale = findAll()
                pointsOfSale.deleteAllFromRealm()
            }
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to delete all PointsOfSale", ex)
            Rollbar.instance().critical(ex, tag)
        }
    }

    fun savePointOfSale(pointOfSale: PointOfSale): PointOfSale? {
        var savedPos: PointOfSale? = null
        create(pointOfSale)

        try {
            savedPos = findById(pointOfSale.id)
            Log.d(tag, "PointOfSale is saved: " + savedPos.toString())
        } catch (ex: KotlinNullPointerException) {
            Log.e(tag, "Null PointOfSale", ex)
            Rollbar.instance().critical(ex, tag)
        }

        return savedPos
    }
}