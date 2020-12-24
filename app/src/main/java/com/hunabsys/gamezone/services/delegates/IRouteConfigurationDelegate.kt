package com.hunabsys.gamezone.services.delegates

interface IRouteConfigurationDelegate {

    fun onRouteConfigurationSuccess()

    fun onRouteConfigurationFailure(error: String)
}