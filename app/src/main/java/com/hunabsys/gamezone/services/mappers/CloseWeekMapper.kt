package com.hunabsys.gamezone.services.mappers

import com.hunabsys.gamezone.helpers.JsonValidationHelper
import org.json.JSONArray
import org.json.JSONObject

class CloseWeekMapper {

    companion object {
        private var finishResults = HashMap<Int, String>()

        fun getGameMachineClosed(): HashMap<Int, String> {
            return finishResults
        }
    }

    fun mapCloseWeeks(closeWeeks: JSONArray): Boolean {
        var closeWeek: JSONObject

        finishResults.clear()

        for (i in 0 until closeWeeks.length()) {
            closeWeek = closeWeeks.getJSONObject(i)
            val gameMachineId = JsonValidationHelper().getIntValue(closeWeek, "game_machine_id")
            var success = JsonValidationHelper().getStringValue(closeWeek, "success")

            if (success.isEmpty()) {
                success = "null"
            }

            finishResults[gameMachineId] = success
        }
        return true
    }
}
