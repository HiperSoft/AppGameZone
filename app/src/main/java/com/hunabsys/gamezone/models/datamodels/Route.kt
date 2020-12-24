package com.hunabsys.gamezone.models.datamodels

import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

/**
 * Route model.
 * Created by Silvia Valdez on 29/01/2018.
 */
// The model has to extend RealmObject.
// Furthermore, the class must be annotated with open (Kotlin classes are final by default).
@RealmClass
open class Route(
        // You can put properties in the constructor as long as all of them are initialized
        // with default values. This ensures that an empty constructor is generated.
        // All properties are by default persisted.
        // Properties can be annotated with @PrimaryKey or @Index.
        // If you use non-nullable types, properties must be initialized with non-null values.
        @PrimaryKey var id: Long = 0,
        var code: String = "",

        // One-to-many relations are simply a RealmList of objects which also subclass RealmObject
        var pointsOfSale: RealmList<PointOfSale> = RealmList()) : RealmObject() {

    // The Kotlin compiler generates standard getters and setters.
    // Realm will overload them and code inside them is ignored.
    // So if you prefer you can also just have empty abstract methods.
}