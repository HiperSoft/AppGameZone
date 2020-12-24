package com.hunabsys.gamezone.models.datamodels
/* ktlint-disable no-wildcard-imports */
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*

/**
 * Prize model
 * Created by Jonathan Hernandez on 23/07/2018
 */
@RealmClass
open class Prize : RealmObject() {

    @PrimaryKey
    var id: Long = 0
    var webId: Long = 0

    var userId: Long = 0
    var routeId: Long = 0
    var pointOfSaleId: Long = 0
    var gameMachineId: Long = 0

    var inputReading: Int = 0
    var outputReading: Int = 0
    var screen: Int = 0
    var prizeAmount: Double = 0.0
    var currentAmount: Double = 0.0
    var toComplete: Double = 0.0
    var gameMachineFund: Double = 0.0
    var expenseAmount: Double = 0.0
    var comments: String = ""

    var week: Int = 0
    var createdAt: Date? = null

    var evidences: RealmList<Evidence> = RealmList()

    var hasSynchronizedData: Boolean = false
    var hasSynchronizedPhotos: Boolean = false
}