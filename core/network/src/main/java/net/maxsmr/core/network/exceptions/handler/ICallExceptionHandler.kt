package net.maxsmr.core.network.exceptions.handler

interface ICallExceptionHandler {

    suspend fun onException(e: RuntimeException)
}