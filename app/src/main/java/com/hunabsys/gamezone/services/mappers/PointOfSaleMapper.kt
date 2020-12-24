package com.hunabsys.gamezone.services.mappers

import com.hunabsys.gamezone.helpers.JsonValidationHelper
import com.hunabsys.gamezone.models.daos.PointOfSaleDao
import com.hunabsys.gamezone.models.datamodels.PointOfSale
import io.realm.RealmList
import org.json.JSONArray
import org.json.JSONObject

/**
 * Maps a JSON Object to a PointOfSale Object and save it into Realm DB.
 * Created by Silvia Valdez on 11/02/2018.
 */
class PointOfSaleMapper {

    private fun mapPointOfSale(point: JSONObject): PointOfSale? {
        val id = JsonValidationHelper().getLongValue(point, "id")
        val name = JsonValidationHelper().getStringValue(point, "pos_name")
        val commission = JsonValidationHelper().getIntValue(point, "sales_comission")
        val gameMachines = GameMachineMapper().mapGameMachines(point)

        val pointOfSale = PointOfSale(id, name, commission, gameMachines)
        return PointOfSaleDao().savePointOfSale(pointOfSale)
    }

    fun mapPointsOfSale(route: JSONObject): RealmList<PointOfSale> {
        val data: JSONArray = route.getJSONArray("point_of_sales")
        val pointsOfSale = RealmList<PointOfSale>()

        (0 until data.length())
                .map { data.getJSONObject(it) }
                .mapTo(pointsOfSale) { PointOfSaleMapper().mapPointOfSale(it) }

        return pointsOfSale
    }
}