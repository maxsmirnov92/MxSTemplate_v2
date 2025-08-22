package net.maxsmr.core.network.exceptions.handler

interface CallExceptionHandler {

    fun onException(e: RuntimeException)
}