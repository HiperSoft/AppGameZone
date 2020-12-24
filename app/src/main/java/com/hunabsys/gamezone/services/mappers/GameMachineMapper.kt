package com.hunabsys.gamezone.services.mappers

import com.hunabsys.gamezone.helpers.JsonValidationHelper
import com.hunabsys.gamezone.models.daos.GameMachineDao
import com.hunabsys.gamezone.models.datamodels.GameMachine
import io.realm.RealmList
import org.json.JSONArray
import org.json.JSONObject

/**
 * Maps a JSON Object to a GameMachine Object and save it into Realm DB.
 * Created by Silvia on 11/02/2018.
 */
class GameMachineMapper {

    private fun mapGameMachine(machine: JSONObject): GameMachine? {
        val id = JsonValidationHelper().getLongValue(machine, "id")
        val folio = JsonValidationHelper().getStringValue(machine, "folio")
        val realFund = JsonValidationHelper().getDoubleValue(machine, "real_fund")
        val week = JsonValidationHelper().getIntValue(machine, "week")

        var lastEntry = 0.0
        var lastOutcome = 0.0
        if (machine.has("last_revision")
                && machine.optJSONObject("last_revision") != null) {
            val lastRevision = machine.getJSONObject("last_revision")

            lastEntry = JsonValidationHelper().getDoubleValue(lastRevision, "input_read")
            lastOutcome = JsonValidationHelper().getDoubleValue(lastRevision, "output_read")
        }

        val gameMachine = GameMachine(id, folio, realFund, week, lastEntry, lastOutcome)
        return GameMachineDao().saveGameMachine(gameMachine)
    }

    fun mapGameMachines(pointOfSale: JSONObject): RealmList<GameMachine> {
        val data: JSONArray = pointOfSale.getJSONArray("game_machines")
        val gameMachines = RealmList<GameMachine>()

        (0 until data.length())
                .map { data.getJSONObject(it) }
                .mapTo(gameMachines) { mapGameMachine(it) }

        return gameMachines
    }
}