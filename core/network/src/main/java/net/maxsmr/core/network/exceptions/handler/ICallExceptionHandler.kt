package net.maxsmr.core.network.exceptions.handler

import net.maxsmr.core.network.exceptions.ApiException

interface ICallExceptionHandler {

    suspend fun onException(e: RuntimeException)
}