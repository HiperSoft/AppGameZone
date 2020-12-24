package com.hunabsys.gamezone.models.datamodels

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

/**
 * Evidence model.
 * Created by Silvia Valdez on 03/06/2018.
 */
@RealmClass
open class Evidence(@PrimaryKey var id: Long = 0,
                    var userId: Long = 0,
                    var evidenceId: Long = 0,
                    var evidenceableId: Long = 0,
                    var evidenceableType: String = "",
                    var file: String = "",
                    var filename: String = "",
                    var originalFilename: String = "",
                    var isSynchronized: Boolean = false
) : RealmObject()