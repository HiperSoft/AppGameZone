package com.hunabsys.gamezone.services.mappers

import android.util.Log
import com.hunabsys.gamezone.helpers.JsonValidationHelper
import com.hunabsys.gamezone.models.daos.GameMachineDao
import com.hunabsys.gamezone.models.daos.PointOfSaleDao
import com.hunabsys.gamezone.models.daos.RouteDao
import com.hunabsys.gamezone.models.datamodels.Route
import org.json.JSONArray
import org.json.JSONObject

/**
 * Maps a JSON Object to a Route Object and save it into Realm DB.
 * Created by Silvia Valdez on 11/02/2018.
 */
class RouteMapper {

    private val tag = RouteMapper::class.simpleName

    fun mapRoute(routeArray: JSONArray): Route? {
        deletePreviousData()

        val route: JSONObject = routeArray.getJSONObject(0)
        val id = JsonValidationHelper().getLongValue(route, "id")
        val code = JsonValidationHelper().getStringValue(route, "code")
        val pointsOfSale = PointOfSaleMapper().mapPointsOfSale(route)

        val routeObject = Route(id, code, pointsOfSale)
        return RouteDao().saveRoute(routeObject)
    }

    private fun deletePreviousData() {
        GameMachineDao().deleteAll()
        PointOfSaleDao().deleteAll()
        RouteDao().deleteAll()

        Log.d(tag, "------- Deleted all previous data!")
    }
}