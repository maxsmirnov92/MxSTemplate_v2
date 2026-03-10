package net.maxsmr.core.network.retrofit.converters

interface OnServerResponseListener {

    fun onServerResponse(errorCode: Int, errorMessage: String /*timestamp: Instant? = null*/)
}