package com.hunabsys.gamezone.services.delegates

/**
 * Default delegate for HttpClientService requests.
 * Created by Silvia Valdez on 21/01/2018.
 */
interface IHttpClientDelegate {

    fun onSuccess(result: ArrayList<Any>)

    fun onFailure(error: String)
}