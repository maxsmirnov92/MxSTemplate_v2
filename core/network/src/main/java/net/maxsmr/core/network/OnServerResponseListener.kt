package net.maxsmr.core.network

import kotlinx.datetime.Instant

interface OnServerResponseListener {

    fun onServerResponse(errorCode: Int, errorMessage: String, timestamp: Instant? = null)
}