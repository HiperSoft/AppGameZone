package com.hunabsys.gamezone.models.datamodels

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

/**
 * PointOfSale model.
 * Created by Silvia Valdez on 29/01/2018.
 */
@RealmClass
open class PointOfSale(@PrimaryKey var id: Long = 0,
                       var name: String? = null,
                       var commissionPercentage: Int = 0,
                       var gameMachines: RealmList<GameMachine> = RealmList(),
                       @LinkingObjects("pointsOfSale")
                       val routes: RealmResults<Route>? = null)
    : RealmObject()