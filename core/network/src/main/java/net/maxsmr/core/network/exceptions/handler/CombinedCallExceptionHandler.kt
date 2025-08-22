package net.maxsmr.core.network.exceptions.handler

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CombinedCallExceptionHandler(
    private val handlers: List<CallExceptionHandler>
) : CallExceptionHandler {

    private val _exceptionsFlow = MutableSharedFlow<RuntimeException>()
    val exceptionsFlow = _exceptionsFlow.asSharedFlow()

    override fun onException(e: RuntimeException) {
        handlers.forEach {
            it.onException(e)
        }
        _exceptionsFlow.tryEmit(e)
    }
}
