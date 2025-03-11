package net.maxsmr.core.android.coroutines.execute

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.states.ILoadState
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.commonutils.states.PgnLoadState
import net.maxsmr.core.network.NO_ERROR_API
import net.maxsmr.core.network.exceptions.NetworkException
import net.maxsmr.core.network.exceptions.OkHttpException.Companion.orNetworkCause
import net.maxsmr.core.network.getErrorCode

sealed class ExecuteResult<out R> {

    data class Success<out T>(val data: T) : ExecuteResult<T>()

    data class Error(val exception: Throwable, val message: TextMessage? = null) : ExecuteResult<Nothing>() {

        /**
         * @return [TextMessage] ошибки, либо null
         */
        fun errorMessage(): TextMessage? {
            return message
                ?: exception.message?.takeIf { it.isNotEmpty() }?.let {
                    TextMessage(it)
                }
        }

        fun errorData(): ILoadState.ErrorData {
            return ILoadState.ErrorData(this.exception, this.message)
        }
    }

    object Loading : ExecuteResult<Nothing>()

    object PgnLoading : ExecuteResult<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[exception=$exception]"
            is Loading -> "Loading"
            is PgnLoading -> "PgnLoading"
        }
    }
}

val ExecuteResult<*>.succeeded
    get() = this is ExecuteResult.Success && data != null

val <T> ExecuteResult<T>.data: T?
    get() = (this as? ExecuteResult.Success)?.data

fun <D> ExecuteResult<D>.hasData(
    dataValidator: ((D) -> Boolean)? = null
): Boolean {
    val data = data
    return data != null && (dataValidator == null || dataValidator(data))
}

fun <D> ExecuteResult<D>.successWithData(
    dataValidator: ((D) -> Boolean)? = null
): Boolean {
    return succeeded && hasData(dataValidator)
}

/**
 * Обновление значения [liveData] если [ExecuteResult] типа [Success]
 */
inline fun <reified T> ExecuteResult<T>.updateOnSuccess(liveData: MutableLiveData<T>) {
    if (this is ExecuteResult.Success) {
        liveData.value = data
    }
}

/**
 * Обновление значения [MutableStateFlow] если [ExecuteResult] типа [Success]
 */
inline fun <reified T> ExecuteResult<T>.updateOnSuccess(stateFlow: MutableStateFlow<T>) {
    if (this is ExecuteResult.Success) {
        stateFlow.value = data
    }
}

fun <T> ILoadState<T>.asExecuteResult() = when {
    isLoading -> {
        if (this is PgnLoadState) {
            if (this.loadingState is PgnLoadState.PgnLoading.PageLoad) {
                ExecuteResult.PgnLoading
            } else {
                ExecuteResult.Loading
            }
        } else {
            ExecuteResult.Loading
        }
    }
    isSuccess() -> ExecuteResult.Success(data)
    else -> ExecuteResult.Error(error?.error ?: Exception(), error?.message as? TextMessage)
}

fun <T, U> ILoadState<T>.asExecuteResult(mapOnSuccess: (data: T) -> U) = when {
    isLoading -> {
        if (this is PgnLoadState) {
            if (this.loadingState is PgnLoadState.PgnLoading.PageLoad) {
                ExecuteResult.PgnLoading
            } else {
                ExecuteResult.Loading
            }
        } else {
            ExecuteResult.Loading
        }
    }
    isSuccess() -> {
        val data = data
        if (data != null) {
            ExecuteResult.Success(mapOnSuccess(data))
        } else {
            ExecuteResult.Error(IllegalStateException())
        }
    }

    else -> ExecuteResult.Error(error?.error ?: Exception(), error?.message as? TextMessage)
}

fun <T> ExecuteResult<T>.asState(data: T? = null): LoadState<T> = when (this) {
    is ExecuteResult.Loading, is ExecuteResult.PgnLoading -> LoadState.loading(data)
    is ExecuteResult.Success -> LoadState.success(this.data)
    is ExecuteResult.Error -> LoadState.error(errorData(), data)
}

fun <T> ExecuteResult<T>.asPgnState(data: T? = null): PgnLoadState<T> = when (this) {
    is ExecuteResult.Loading -> PgnLoadState.pgnLoading(PgnLoadState.PgnLoading.MainLoad, data)
    is ExecuteResult.PgnLoading -> PgnLoadState.pgnLoading(PgnLoadState.PgnLoading.PageLoad, data)
    is ExecuteResult.Success -> PgnLoadState.pgnSuccess(this.data, true)
    is ExecuteResult.Error -> PgnLoadState.pgnError(errorData(), data)
}

fun <T, U> ExecuteResult<T>.mapData(
    mapData: (data: T) -> U
): ExecuteResult<U> = when (this) {
    is ExecuteResult.Loading -> ExecuteResult.Loading
    is ExecuteResult.PgnLoading -> ExecuteResult.PgnLoading
    is ExecuteResult.Success -> {
        try {
            ExecuteResult.Success(mapData(this.data))
        } catch (e: Exception) {
            ExecuteResult.Error(e)
        }
    }
    is ExecuteResult.Error -> ExecuteResult.Error(this.exception, this.message)
}

fun <T> ExecuteResult<T>?.isNetworkError(): Boolean {
    return this is ExecuteResult.Error && this.exception is NetworkException
}

fun <T> ExecuteResult<T>.getErrorCode(): Int = when (this) {
    is ExecuteResult.Error -> exception.getErrorCode()
    else -> NO_ERROR_API
}

fun <T> Flow<T>.asExecuteResult(): Flow<ExecuteResult<T>> {
    return this
        .map<T, ExecuteResult<T>> {
            ExecuteResult.Success(it)
        }
        .onStart { emit(ExecuteResult.Loading) }
        .catch { emit(it.asExecuteResult()) }
}

fun <T> Throwable.asExecuteResult(): ExecuteResult<T> {
    return ExecuteResult.Error(orNetworkCause())
}