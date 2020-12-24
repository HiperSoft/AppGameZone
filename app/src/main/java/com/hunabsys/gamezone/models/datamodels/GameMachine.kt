package com.hunabsys.gamezone.models.datamodels

import io.realm.RealmObject
import io.realm.RealmResults
import io.realm.annotations.LinkingObjects
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

/**
 * GameMachine model.
 * Created by Silvia Valdez on 29/01/2018.
 */
@RealmClass
open class GameMachine(@PrimaryKey var id: Long = 0,
                       var folio: String = "",
                       var realFund: Double = 0.0,
                       var week: Int = 0,
                       var lastEntry: Double = 0.0,
                       var lastOutcome: Double = 0.0,
                       @LinkingObjects("gameMachines")
                       val pointsOfSale: RealmResults<PointOfSale>? = null)
    : RealmObject()