package com.hunabsys.gamezone.models.daos

import android.util.Log
import com.hunabsys.gamezone.models.datamodels.Route
import com.hunabsys.gamezone.services.rest.HttpClientService
import com.rollbar.android.Rollbar
import io.realm.Realm
import io.realm.RealmResults

/**
 * Data access object for Route.
 * Created by Silvia Valdez on 2/9/18.
 */
class RouteDao {

    private val tag = RouteDao::class.simpleName
    private val realm = Realm.getDefaultInstance()

    private fun create(route: Route) {
        try {
            realm.executeTransaction {
                val routeObject = realm.createObject(Route::class.java, route.id)
                routeObject.code = route.code
                routeObject.pointsOfSale = route.pointsOfSale
            }
        } catch (ex: IllegalArgumentException) {
            Log.e(tag, "Attempting to create Route", ex)
            Rollbar.instance().critical(ex, tag)
        }
    }

    private fun findById(id: Long): Route {
        return realm.where(Route::class.java).equalTo("id", id).findFirst()!!
    }

    fun findAll(): RealmResults<Route> {
        return realm.where(Route::class.java).findAll()!!
    }

    fun deleteAll() {
        try {
            realm.executeTransaction {
                val routes = findAll()
                routes.deleteAllFromRealm()
            }
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to delete all Routes", ex)
            Rollbar.instance().critical(ex, tag)
        }
    }

    fun saveRoute(route: Route): Route? {
        var savedRoute: Route? = null
        create(route)

        try {
            savedRoute = findById(route.id)
            Log.d(tag, "Route is saved: " + savedRoute.toString())
            if (savedRoute.toString().contains("<PointOfSale>[0]")) {
                HttpClientService.userPointOfSaleUnsigned = true
            }
        } catch (ex: KotlinNullPointerException) {
            Log.e(tag, "Null Route", ex)
            Rollbar.instance().critical(ex, tag)
        }

        return savedRoute
    }
}