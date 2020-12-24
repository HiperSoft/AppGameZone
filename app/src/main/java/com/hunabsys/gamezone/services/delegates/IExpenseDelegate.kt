package com.hunabsys.gamezone.services.delegates

interface IExpenseDelegate {

    fun onExpenseSuccess(webId: Long)

    fun onExpenseFailure(error: String)
}