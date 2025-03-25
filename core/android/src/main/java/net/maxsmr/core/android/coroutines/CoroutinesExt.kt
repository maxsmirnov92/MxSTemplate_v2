package net.maxsmr.core.android.coroutines

import android.os.Handler
import android.os.HandlerThread
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow

fun <T> MutableStateFlow<Collection<T>>.appendToList(newValue: T) {
    val list = value.toMutableList()
    list.add(newValue)
    tryEmit(list)
}

fun <T> MutableStateFlow<Set<T>>.appendToSet(newValue: T) {
    val list = value.toMutableSet()
    list.add(newValue)
    tryEmit(list)
}


fun HandlerThread.asDispatcher(): CoroutineDispatcher {
    return this
        .apply { start() }
        .looper.let { Handler(it) }
        .asCoroutineDispatcher()
}