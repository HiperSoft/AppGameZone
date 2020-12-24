package com.hunabsys.gamezone.services.delegates

interface ICheckInDelegate {

    fun onCheckInSuccess()

    fun onCheckInFailure(error: String)
}