package com.hunabsys.gamezone.services.delegates

interface ISaleRevisionDelegate {

    fun onSaleRevisionSuccess(webId: Long)

    fun onSaleRevisionFailure(error: String)
}