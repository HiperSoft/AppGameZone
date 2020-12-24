package com.hunabsys.gamezone.services.delegates

interface ILoginDelegate {

    fun onLoginSuccess()

    fun onLoginFailure(error: String)
}