package net.maxsmr.core.network.exceptions.handler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import net.maxsmr.core.network.exceptions.ApiException

class CombinedApiExceptionHandler(private val handlers: List<IApiExceptionHandler>) : IApiExceptionHandler {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _exceptionsFlow = MutableSharedFlow<ApiException>()
    val exceptionsFlow = _exceptionsFlow.asSharedFlow()

    override fun onApiException(e: ApiException) {
        handlers.forEach {
            it.onApiException(e)
        }
        scope.launch {
            _exceptionsFlow.emit(e)
        }
    }
}