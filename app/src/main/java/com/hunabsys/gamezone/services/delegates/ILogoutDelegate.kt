package com.hunabsys.gamezone.services.delegates

interface ILogoutDelegate {

    fun onLogoutSuccess()

    fun onLogoutFailure(error: String)
}