package net.maxsmr.core.network.exceptions.handler

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class CombinedCallExceptionHandler(
    private val handlers: List<ICallExceptionHandler>,
) : ICallExceptionHandler {

    val exceptionsFlow: SharedFlow<RuntimeException> by lazy {
        _exceptionsFlow.asSharedFlow()
    }

    private val _exceptionsFlow = MutableSharedFlow<RuntimeException>()

    override suspend fun onException(e: RuntimeException) {
        handlers.forEach {
            it.onException(e)
        }
        _exceptionsFlow.emit(e)
    }
}
