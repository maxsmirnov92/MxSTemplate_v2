package net.maxsmr.core.network.exceptions.handler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import net.maxsmr.core.network.exceptions.ApiException

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
