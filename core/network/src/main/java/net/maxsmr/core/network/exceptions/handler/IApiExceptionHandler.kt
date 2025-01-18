package net.maxsmr.core.network.exceptions.handler

import net.maxsmr.core.network.exceptions.ApiException

interface IApiExceptionHandler {

    fun onApiException(e: ApiException)
}