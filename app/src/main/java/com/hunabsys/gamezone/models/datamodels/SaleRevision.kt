package com.hunabsys.gamezone.models.datamodels
/* ktlint-disable no-wildcard-imports */
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*

/**
 * SaleRevision model.
 * Created by Silvia Valdez on 2/13/18.
 */
@RealmClass
open class SaleRevision : RealmObject() {

    // For this case it's required also an empty constructor
    // to be able to use a custom constructor for SaleRevisionsListAdapter class.

    @PrimaryKey
    var id: Long = 0
    var webId: Long = 0

    var userId: Long = 0
    var routeId: Long = 0
    var pointOfSaleId: Long = 0
    var gameMachineId: Long = 0

    var entry: Int = 0
    var outcome: Int = 0
    var screen: Int = 0
    var gameMachineOutcome: Double = 0.0
    var commissionAmount: Double = 0.0
    var commissionPercentage: Int = 0
    var currentFund: Double = 0.0
    var comments: String = ""
    var latitude: Double = 0.0
    var longitude: Double = 0.0

    var week: Int = 0
    var createdAt: Date? = null

    var evidences: RealmList<Evidence> = RealmList()

    var hasSynchronizedData: Boolean = false
    var hasSynchronizedPhotos: Boolean = false
}
