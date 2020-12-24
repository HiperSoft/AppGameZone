package com.hunabsys.gamezone.services.mappers

import android.content.Context
import android.util.Log
import com.hunabsys.gamezone.helpers.JsonValidationHelper
import com.hunabsys.gamezone.helpers.PreferencesHelper
import org.json.JSONObject

class UserMapper {

    private val tag = UserMapper::class.simpleName

    fun mapUser(context: Context, result: JSONObject): Boolean {
        if (result.has("data")) {
            val data = result.getJSONObject("data")
            val id = JsonValidationHelper().getLongValue(data, "id")
            val email = JsonValidationHelper().getStringValue(data, "email")
            val userName = JsonValidationHelper().getStringValue(data, "username")

            PreferencesHelper(context).userId = id
            PreferencesHelper(context).email = email
            PreferencesHelper(context).userName = userName

            Log.d(tag, "------- User ID: $id")
            return true
        } else {
            Log.e(tag, "No key 'data' was found in: $result")
        }
        return false
    }
}