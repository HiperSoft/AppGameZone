package com.hunabsys.gamezone.services.rest

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.result.Result
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.PreferencesHelper
import com.hunabsys.gamezone.services.delegates.IHttpClientDelegate
import com.rollbar.android.Rollbar
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.io.Reader
import java.net.HttpURLConnection
import java.nio.charset.Charset

/**
 * Helper class for HTTP networking.
 * Created by Silvia Valdez on 20/01/2018.
 */
class HttpClientService(private val context: Context,
                        private val delegate: IHttpClientDelegate) {

    private val tag = HttpClientService::class.simpleName

    companion object {
        var sessionUnauthorized = false
        var userRoutesUnsigned = false
        var userPointOfSaleUnsigned = false
    }

    fun getServer(): String {
//        return IServicesConstants.DUMMY_SERVER
        return IServicesConstants.DEVELOPMENT_SERVER
//        return IServicesConstants.PRODUCTION_SERVER
//        return IServicesConstants.EDUARDO_SERVER
    }

    fun networkAvailable(): Boolean {
        val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting
    }

    fun get(url: String) {
        val preferencesHelper = PreferencesHelper(context)

        Fuel.get(url)
                .header("Content-Type" to "application/json",
                        "uid" to preferencesHelper.uId,
                        "client" to preferencesHelper.client,
                        "access-token" to preferencesHelper.token)
                .timeout(IServicesConstants.TIMEOUT)
                .response { request, response, result ->
                    Log.d(tag, url)
                    Log.d(tag, "Request: " + request.toString())

                    when (result) {
                        is Result.Failure -> {
                            val (_, error) = result
                            if (error != null) {
                                Log.e(tag, "STATUS CODE: ${response.statusCode}")
                                Log.e(tag, "RESPONSE: ${error.message}")

                                var message = ""
                                if (error.message != null) {
                                    message = (error.message).toString()
                                }

                                when (response.statusCode) {
                                    400 -> {
                                        Log.e(tag, "400 Bad Request - " +
                                                "$url \n $message")
                                    }
                                    401 -> {
                                        sessionUnauthorized = true
                                    }
                                    404 -> {
                                        Log.e(tag, "404 Not Found - $url \n" +
                                                " $message")
                                    }
                                    500 -> {
                                        Rollbar.instance().info("$tag - 500 Internal Server " +
                                                "Error - $url \n $message")
                                    }
                                    else -> {
                                        Rollbar.instance().info("$tag - ${response.statusCode}" +
                                                " - $url \n $message")
                                    }
                                }

                                val errorMessage = getErrorMessage(response.statusCode)
                                delegate.onFailure(errorMessage)
                            }
                        }
                        is Result.Success -> {
                            saveHeaders(response.headers)

                            val data = validateResponse(response)
                            delegate.onSuccess(data)
                        }
                    }
                }
    }

    fun post(url: String, content: String) {
        val preferencesHelper = PreferencesHelper(context)

        Fuel.post(url)
                .header("Content-Type" to "application/json",
                        "uid" to preferencesHelper.uId,
                        "client" to preferencesHelper.client,
                        "access-token" to preferencesHelper.token)
                .body(content, Charset.defaultCharset())
                .timeout(IServicesConstants.TIMEOUT)
                .response { request, response, result ->
                    Log.d(tag, url)
                    Log.d(tag, "Request: " + request.toString())

                    when (result) {
                        is Result.Failure -> {
                            val (_, error) = result
                            if (error != null) {
                                Log.e(tag, "STATUS CODE: ${response.statusCode}")
                                Log.e(tag, "ERROR MESSAGE: ${error.message}")

                                var message = ""
                                if (error.message != null) {
                                    message = (error.message).toString()
                                }

                                when (response.statusCode) {
                                    400 -> {
                                        Log.e(tag, "400 Bad Request - " +
                                                "$url \n $message")
                                    }
                                    401 -> {
                                        sessionUnauthorized = true
                                    }
                                    404 -> {
                                        Log.e(tag, "404 Not Found - $url \n" +
                                                " $message")
                                    }
                                    500 -> {
                                        Rollbar.instance().info("$tag - 500 Internal Server " +
                                                "Error - $url \n $message")
                                    }
                                    else -> {
                                        Rollbar.instance().info("$tag - ${response.statusCode}" +
                                                " - $url \n $message")
                                    }
                                }

                                val errorMessage = getErrorMessage(response.statusCode)
                                delegate.onFailure(errorMessage)
                            }
                        }
                        is Result.Success -> {
                            saveHeaders(response.headers)

                            val data = validateResponse(response)
                            delegate.onSuccess(data)
                        }
                    }
                }
    }

    fun delete(url: String) {
        val preferencesHelper = PreferencesHelper(context)

        val uid = preferencesHelper.uId
        val client = preferencesHelper.client
        val token = preferencesHelper.token
        val expiry = preferencesHelper.expiry

        Fuel.delete(url)
                .header("Content-Type" to "application/json",
                        "uid" to uid,
                        "client" to client,
                        "access-token" to token,
                        "expiry" to expiry)
                .response { request, response, result ->
                    Log.d(tag, url)
                    Log.d(tag, "Request: " + request.toString())

                    when (result) {
                        is Result.Failure -> {
                            val (_, error) = result
                            if (error != null) {
                                Log.e(tag, "STATUS CODE: ${response.statusCode}")
                                Log.e(tag, "ERROR MESSAGE: ${error.message}")

                                var message = ""
                                if (error.message != null) {
                                    message = (error.message).toString()
                                }

                                when (response.statusCode) {
                                    400 -> {
                                        Log.e(tag, "400 Bad Request - " +
                                                "$url \n $message")
                                    }
                                    401 -> {
                                        sessionUnauthorized = true
                                    }
                                    404 -> {
                                        Log.e(tag, "404 Not Found - $url \n" +
                                                " $message")
                                    }
                                    500 -> {
                                        Rollbar.instance().info("$tag - 500 Internal Server " +
                                                "Error - $url \n $message")
                                    }
                                    else -> {
                                        Rollbar.instance().info("$tag - ${response.statusCode}" +
                                                " - $url \n $message")
                                    }
                                }

                                val errorMessage = getErrorMessage(response.statusCode)
                                delegate.onFailure(errorMessage)
                            }
                        }
                        is Result.Success -> {
                            val data = validateResponse(response)
                            delegate.onSuccess(data)
                        }
                    }
                }
    }

    private fun getErrorMessage(responseCode: Int): String {
        return when (responseCode) {
            HttpURLConnection.HTTP_UNAUTHORIZED -> {
                context.getString(R.string.error_unauthorized) // 401
            }
            HttpURLConnection.HTTP_NOT_FOUND -> {
                context.getString(R.string.error_not_found) // 404
            }

            else -> context.getString(R.string.error_default)
        }
    }

    private fun saveHeaders(headers: Map<String, List<String>>) {
        if (headers.containsKey("uid")) {
            val uid = headers["uid"]!![0]
            val client = headers["client"]!![0]
            val accessToken = headers["access-token"]!![0]
            val expiry = headers["expiry"]!![0]
            Log.d(tag, "Received headers --> uid: $uid, client: $client, access-token: " +
                    "$accessToken, expiry: $expiry")
            PreferencesHelper(context).saveSession(uid, client, accessToken, expiry)
        }
    }

    private fun validateResponse(response: Response): ArrayList<Any> {
        // Get response's status code
        val result = ArrayList<Any>()
        result.add(response.statusCode)

        try {
            // Get response data
            val inputStream = ByteArrayInputStream(response.data)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream) as Reader?)

            var line = bufferedReader.readLine()
            val stringBuilder = StringBuilder()

            while (line != null) {
                stringBuilder.append(line)
                line = bufferedReader.readLine()
            }
            bufferedReader.close()

            parseResponse(result, stringBuilder.toString())

            Log.d(tag, "STATUS CODE: ${response.statusCode}")
            Log.d(tag, "RESPONSE: ${result[1]}")
        } catch (ex: JSONException) {
            Log.e(tag, "Attempting to decode server's response as JSONObject", ex)
        }

        return result
    }

    private fun parseResponse(result: ArrayList<Any>, data: String) {
        try {
            val response = JSONObject(data)
            result.add(response)
        } catch (ex: Exception) {
            Log.d(tag, "JSONArray cannot be converted to JSONObject")

            val response = JSONArray(data)
            result.add(response)
        }
    }
}