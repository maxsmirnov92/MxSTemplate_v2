package net.maxsmr.core.network.exceptions.handler

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CombinedCallExceptionHandler(
    private val handlers: List<ICallExceptionHandler>
) : ICallExceptionHandler {

    private val _exceptionsFlow = MutableSharedFlow<RuntimeException>()
    val exceptionsFlow = _exceptionsFlow.asSharedFlow()

    override suspend fun onException(e: RuntimeException) {
        handlers.forEach {
            it.onException(e)
        }
        _exceptionsFlow.emit(e)
    }
}
