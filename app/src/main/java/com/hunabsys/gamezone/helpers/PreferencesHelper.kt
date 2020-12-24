package com.hunabsys.gamezone.helpers

import android.content.Context
import android.content.SharedPreferences

/**
 * Helper class for SharedPreferences management.
 * Created by Silvia Valdez on 1/19/18.
 */

const val PREFS_FILE_NAME = "com.hunabsys.gamezone.prefs"

const val PREFS_USER_ID = "PREF_USER_ID"
const val PREFS_SAVED_CONFIG = "PREFS_SAVED_CONFIG"

const val PREFS_SESSION_ACTIVE = "PREFS_SESSION_ACTIVE"
const val PREFS_SESSION_EXPIRATION = "PREFS_SESSION_EXPIRATION"
const val PREFS_SESSION_UID = "PREFS_SESSION_UID"
const val PREFS_SESSION_CLIENT = "PREFS_SESSION_CLIENT"
const val PREFS_SESSION_TOKEN = "PREFS_SESSION_TOKEN"

const val PREFS_STORAGE_ALARM = "PREFS_STORAGE_ALARM"

class PreferencesHelper(context: Context) {

    private val sharedPrefs: SharedPreferences =
            context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    var savedConfig: Boolean
        get() = sharedPrefs.getBoolean(PREFS_SAVED_CONFIG, false)
        set(saved) = sharedPrefs.edit().putBoolean(PREFS_SAVED_CONFIG, saved).apply()

    var activeSession: Boolean
        get() = sharedPrefs.getBoolean(PREFS_SESSION_ACTIVE, false)
        set(active) = sharedPrefs.edit().putBoolean(PREFS_SESSION_ACTIVE, active).apply()

    var userId: Long
        get() = sharedPrefs.getLong(PREFS_USER_ID, 0)
        set(id) = sharedPrefs.edit().putLong(PREFS_USER_ID, id).apply()

    var uId: String
        get() = sharedPrefs.getString(PREFS_SESSION_UID, "")
        set(id) = sharedPrefs.edit().putString(PREFS_SESSION_UID, id).apply()

    var client: String
        get() = sharedPrefs.getString(PREFS_SESSION_CLIENT, "")
        set(strClient) = sharedPrefs.edit().putString(PREFS_SESSION_CLIENT, strClient).apply()

    var token: String
        get() = sharedPrefs.getString(PREFS_SESSION_TOKEN, "")
        set(strToken) = sharedPrefs.edit().putString(PREFS_SESSION_TOKEN, strToken).apply()

    var expiry: String
        get() = sharedPrefs.getString(PREFS_SESSION_EXPIRATION, "")
        set(expiration) = sharedPrefs.edit().putString(PREFS_SESSION_EXPIRATION, expiration).apply()

    var email: String
        get() = sharedPrefs.getString(PREFS_SESSION_EXPIRATION, "")
        set(expiration) = sharedPrefs.edit().putString(PREFS_SESSION_EXPIRATION, expiration).apply()

    var userName: String
        get() = sharedPrefs.getString(PREFS_SESSION_EXPIRATION, "")
        set(expiration) = sharedPrefs.edit().putString(PREFS_SESSION_EXPIRATION, expiration).apply()

    var storageAlarm: Boolean
        get() = sharedPrefs.getBoolean(PREFS_STORAGE_ALARM, false)
        set(active) = sharedPrefs.edit().putBoolean(PREFS_STORAGE_ALARM, active).apply()

    fun saveSession(uid: String, client: String, accessToken: String, expiry: String) {
        this.activeSession = true

        if (!uid.isEmpty()) {
            this.uId = uid
        }
        if (!client.isEmpty()) {
            this.client = client
        }
        if (!accessToken.isEmpty()) {
            this.token = accessToken
        }
        if (!expiry.isEmpty()) {
            this.expiry = expiry
        }
    }

    fun dropPreferences() {
        savedConfig = false
        activeSession = false
        userId = 0
        uId = ""
        client = ""
        token = ""
        expiry = ""
        email = ""
        userName = ""
    }
}