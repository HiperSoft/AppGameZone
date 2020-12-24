package com.hunabsys.gamezone.services.mappers

import android.util.Log
import org.json.JSONObject

class LogoutMapper {

    private val tag = LogoutMapper::class.simpleName

    fun mapLogout(result: JSONObject): Boolean {
        if (result.has("success")) {
            Log.d(tag, "------- Logout ID: $result")
            return true
        } else {
            Log.e(tag, "No key 'success' was found in: $result")
        }
        return false
    }
}