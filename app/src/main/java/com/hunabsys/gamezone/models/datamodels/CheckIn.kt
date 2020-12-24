package com.hunabsys.gamezone.models.datamodels
/* ktlint-disable no-wildcard-imports */
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.*

open class CheckIn : RealmObject() {

    //For this case it's required also an empty constructor
    //to be able to use a custom constructor for checkInActivity class

    @PrimaryKey
    var id: Long = 0
    var userId: Long = 0

    var pointOfSaleId: Long = 0

    var latitude: Double = 0.0
    var longitude: Double = 0.0

    var createdAt: Date? = null
    var isSynchronized: Boolean = false
}