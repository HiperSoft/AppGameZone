package com.hunabsys.gamezone.services.delegates

interface IEvidenceDelegate {

    fun onEvidenceSuccess()

    fun onEvidenceFailure(error: String)
}