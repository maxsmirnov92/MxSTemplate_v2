package net.maxsmr.core.network

interface OnServerResponseListener {

    fun onServerResponse(errorCode: Int, errorMessage: String /*timestamp: Instant? = null*/)
}