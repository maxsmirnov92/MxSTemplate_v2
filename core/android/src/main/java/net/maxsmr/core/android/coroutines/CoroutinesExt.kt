package net.maxsmr.core.android.coroutines

import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun <T> Flow<T>.mutableStateIn(
    scope: CoroutineScope,
    initialValue: T,
    context: CoroutineContext = EmptyCoroutineContext
): MutableStateFlow<T> {
    val flow = MutableStateFlow(initialValue)
    scope.launch(context) {
        this@mutableStateIn.collect(flow)
    }
    return flow
}

fun <T> Flow<T>.mutableSharedStateIn(
    scope: CoroutineScope,
    context: CoroutineContext = EmptyCoroutineContext,
): MutableSharedFlow<T> {
    val flow = MutableSharedFlow<T>()
    scope.launch(context) {
        collect { flow.emit(it) }
    }
    return flow
}

fun <T> MutableStateFlow<Collection<T>>.appendToList(newValue: T) {
    val list = value.toMutableList()
    list.add(newValue)
    value = list
}

fun <T> MutableStateFlow<Set<T>>.appendToSet(newValue: T) {
    val list = value.toMutableSet()
    list.add(newValue)
    value = list
}

fun <T> MutableStateFlow<T>.recharge() {
    tryEmit(value)
}

fun HandlerThread.asDispatcher(): CoroutineDispatcher {
    return this
        .apply { start() }
        .looper.let { Handler(it) }
        .asCoroutineDispatcher()
}