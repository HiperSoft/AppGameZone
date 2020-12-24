package com.hunabsys.gamezone.services.delegates

interface IPrizeDelegate {

    fun onPrizeSuccess(webId: Long)

    fun onPrizeFailure(error: String)
}