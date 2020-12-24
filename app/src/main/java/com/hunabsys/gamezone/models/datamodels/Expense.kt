package com.hunabsys.gamezone.models.datamodels
/* ktlint-disable no-wildcard-imports */
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.util.*

/**
 * Expense model
 * Created by Jonathan Hernandez on 03/08/2018
 */
@RealmClass
open class Expense : RealmObject() {

    @PrimaryKey
    var id: Long = 0
    var webId: Long = 0

    var userId: Long = 0
    var pointOfSaleId: Long = 0

    var concept: String = ""
    var amount: Double = 0.0
    var comments: String = ""

    var week: Int = 0
    var createdAt: Date? = null
    var evidence: Evidence? = null

    var isSynchronized: Boolean = false
}