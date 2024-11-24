package net.maxsmr.core.android.coroutines

import android.os.Handler
import android.os.HandlerThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.live.event.VmEvent
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

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
    tryEmit(list)
}

fun <T> MutableStateFlow<Set<T>>.appendToSet(newValue: T) {
    val list = value.toMutableSet()
    list.add(newValue)
    tryEmit(list)
}

inline fun <T : Any> Flow<T>.collectWithOwner(
    owner: LifecycleOwner,
    lifecycleState: Lifecycle.State = Lifecycle.State.RESUMED,
    crossinline action: suspend (value: T) -> Unit,
) {
    repeatOnLifecycle(owner, lifecycleState) { collectLatest { action(it) } }
}

/**
 * Использовать например в VM или любом другом месте, где LifecycleOwner не требуется
 */
inline fun <T : Any> StateFlow<VmEvent<T>?>.collectEvents(
    scope: CoroutineScope,
    crossinline action: (value: T) -> Unit
) {
    scope.launch {
        collectLatest { event ->
            event?.get(true)?.let {
                action(it)
            }
        }
    }
}

inline fun <T : Any> StateFlow<VmEvent<T>?>.collectEventsWithOwner(
    owner: LifecycleOwner,
    lifecycleState: Lifecycle.State = Lifecycle.State.RESUMED,
    crossinline action: suspend (value: T) -> Unit,
) {
    repeatOnLifecycle(owner, lifecycleState) { collectLatest { event ->
        event?.get(true)?.let {
            action(it)
        }
    } }
}

inline fun repeatOnLifecycle(
    owner: LifecycleOwner,
    lifecycleState: Lifecycle.State,
    crossinline action: suspend () -> Unit,
) {
    owner.lifecycleScope.launch {
        owner.repeatOnLifecycle(lifecycleState) {
            action()
        }
    }
}

fun HandlerThread.asDispatcher(): CoroutineDispatcher {
    return this
        .apply { start() }
        .looper.let { Handler(it) }
        .asCoroutineDispatcher()
}

/**
 * @return холодный flow с периодическими эмитами
 * (задержка не фиксированная - меняется в зав-ти от времени, потраченного на обработку элемента)
 */
fun tickerFlow(period: Duration, initialDelay: Duration = Duration.ZERO) = flow {
    delay(initialDelay)
    while (true) {
        emit(Unit)
        delay(period)
    }
}