package com.hunabsys.gamezone.services.delegates

interface ICloseWeekDelegate {

    fun onCloseWeekSuccess()

    fun onCloseWeekFailure(error: String)
}