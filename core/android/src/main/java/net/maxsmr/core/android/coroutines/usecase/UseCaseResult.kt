package net.maxsmr.core.android.coroutines.usecase

import androidx.annotation.StringRes
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

sealed class UseCaseResult<out R> {

    data class Success<out T>(val data: T) : UseCaseResult<T>()

    data class Error(val exception: Throwable, val message: TextMessage? = null) : UseCaseResult<Nothing>() {

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

    object Loading : UseCaseResult<Nothing>()

    object PgnLoading : UseCaseResult<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[exception=$exception]"
            is Loading -> "Loading"
            is PgnLoading -> "PgnLoading"
        }
    }
}

val UseCaseResult<*>.succeeded
    get() = this is UseCaseResult.Success && data != null

fun <T> UseCaseResult<T>.successOr(fallback: T): T {
    return (this as? UseCaseResult.Success<T>)?.data ?: fallback
}

val <T> UseCaseResult<T>.data: T?
    get() = (this as? UseCaseResult.Success)?.data

/**
 * Обновление значения [liveData] если [UseCaseResult] типа [Success]
 */
inline fun <reified T> UseCaseResult<T>.updateOnSuccess(liveData: MutableLiveData<T>) {
    if (this is UseCaseResult.Success) {
        liveData.value = data
    }
}

/**
 * Обновление значения [MutableStateFlow] если [UseCaseResult] типа [Success]
 */
inline fun <reified T> UseCaseResult<T>.updateOnSuccess(stateFlow: MutableStateFlow<T>) {
    if (this is UseCaseResult.Success) {
        stateFlow.value = data
    }
}

fun <T> ILoadState<T>.asUseCaseResult() = when {
    isLoading -> {
        if (this is PgnLoadState) {
            if (this.loadingState is PgnLoadState.PgnLoading.PageLoad) {
                UseCaseResult.PgnLoading
            } else {
                UseCaseResult.Loading
            }
        } else {
            UseCaseResult.Loading
        }
    }
    isSuccess() -> UseCaseResult.Success(data)
    else -> UseCaseResult.Error(error?.error ?: Exception(), error?.message as? TextMessage)
}

fun <T, U> ILoadState<T>.asUseCaseResult(mapOnSuccess: (data: T) -> U) = when {
    isLoading -> {
        if (this is PgnLoadState) {
            if (this.loadingState is PgnLoadState.PgnLoading.PageLoad) {
                UseCaseResult.PgnLoading
            } else {
                UseCaseResult.Loading
            }
        } else {
            UseCaseResult.Loading
        }
    }
    isSuccess() -> {
        val data = data
        if (data != null) {
            UseCaseResult.Success(mapOnSuccess(data))
        } else {
            UseCaseResult.Error(IllegalStateException())
        }
    }

    else -> UseCaseResult.Error(error?.error ?: Exception(), error?.message as? TextMessage)
}

fun <T> UseCaseResult<T>.asState(data: T? = null): LoadState<T> = when (this) {
    is UseCaseResult.Loading, is UseCaseResult.PgnLoading -> LoadState.loading(data)
    is UseCaseResult.Success -> LoadState.success(this.data)
    is UseCaseResult.Error -> LoadState.error(errorData(), data)
}

fun <T> UseCaseResult<T>.asPgnState(data: T? = null): PgnLoadState<T> = when (this) {
    is UseCaseResult.Loading -> PgnLoadState.pgnLoading(PgnLoadState.PgnLoading.MainLoad, data)
    is UseCaseResult.PgnLoading -> PgnLoadState.pgnLoading(PgnLoadState.PgnLoading.PageLoad, data)
    is UseCaseResult.Success -> PgnLoadState.pgnSuccess(this.data, true)
    is UseCaseResult.Error -> PgnLoadState.pgnError(errorData(), data)
}

fun <T, U> UseCaseResult<T>.mapData(mapData: (data: T) -> U): UseCaseResult<U> = when (this) {
    is UseCaseResult.Loading -> UseCaseResult.Loading
    is UseCaseResult.PgnLoading -> UseCaseResult.PgnLoading
    is UseCaseResult.Success -> UseCaseResult.Success(mapData(this.data))
    is UseCaseResult.Error -> UseCaseResult.Error(this.exception, this.message)
}

fun <T> UseCaseResult<T>?.isNetworkError(): Boolean {
    return this is UseCaseResult.Error && this.exception is NetworkException
}

fun <T> UseCaseResult<T>.getErrorCode(): Int = when (this) {
    is UseCaseResult.Error -> exception.getErrorCode()
    else -> NO_ERROR_API
}

fun <T> Flow<T>.asUseCaseResult(): Flow<UseCaseResult<T>> {
    return this
        .map<T, UseCaseResult<T>> {
            UseCaseResult.Success(it)
        }
        .onStart { emit(UseCaseResult.Loading) }
        .catch { emit(it.asUseCaseResult()) }
}

fun <T> Throwable.asUseCaseResult(): UseCaseResult<T> {
    return UseCaseResult.Error(orNetworkCause())
}