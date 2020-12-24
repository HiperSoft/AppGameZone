package com.hunabsys.gamezone.services.mappers

import android.util.Log
import com.hunabsys.gamezone.helpers.JsonValidationHelper
import com.hunabsys.gamezone.models.daos.CheckInDao
import org.json.JSONArray
import org.json.JSONObject

class CheckInMapper {

    private val tag = CheckInMapper::class.simpleName

    private fun mapCheckIn(checkIn: JSONObject): Boolean {
        val id = JsonValidationHelper().getLongValue(checkIn, "mobile_id")
        val success = JsonValidationHelper().getBooleanValue(checkIn, "success")
        val error = JsonValidationHelper().getStringValue(checkIn, "error_message")

        if (success) {
            val checkInObject = CheckInDao().findCopyById(id)
            checkInObject.isSynchronized = true
            CheckInDao().update(checkInObject)
            return true
        } else {
            if (error != "") {
                Log.e(tag, error)
            }
        }
        return false
    }

    fun mapCheckIns(checkIns: JSONArray): Boolean {
        var result = true
        var success: Boolean
        var checkIn: JSONObject

        for (i in 0 until checkIns.length()) {
            checkIn = checkIns.getJSONObject(i)
            success = mapCheckIn(checkIn)

            if (!success) {
                result = false
            }
        }
        return result
    }
}