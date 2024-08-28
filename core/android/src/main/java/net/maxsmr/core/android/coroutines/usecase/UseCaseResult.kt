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
import net.maxsmr.core.network.NO_ERROR_API
import net.maxsmr.core.network.exceptions.ApiException
import net.maxsmr.core.network.exceptions.NetworkException
import net.maxsmr.core.network.exceptions.NoConnectivityException
import net.maxsmr.core.network.getErrorCode
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException


sealed class UseCaseResult<out R> {

    data class Success<out T>(val data: T) : UseCaseResult<T>()
    data class Error(val exception: Throwable, @StringRes val messageResId: Int = 0) : UseCaseResult<Nothing>() {

        /**
         * @return [TextMessage] ошибки, либо null
         */
        fun errorMessage(): TextMessage? {
            return if (messageResId > 0) {
                TextMessage(messageResId)
            } else if (!exception.message.isNullOrBlank()) {
                TextMessage(exception.message.orEmpty())
            } else {
                null
            }
        }
    }

    object Loading : UseCaseResult<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[exception=$exception]"
            Loading -> "Loading"
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
    isLoading -> UseCaseResult.Loading
    isSuccess() -> UseCaseResult.Success(data)
    else -> UseCaseResult.Error(error ?: Exception())
}

fun <T, U> ILoadState<T>.asUseCaseResult(mapOnSuccess: (data: T) -> U) = when {
    isLoading -> UseCaseResult.Loading
    isSuccess() -> {
        val data = data
        if (data != null) {
            UseCaseResult.Success(mapOnSuccess(data))
        } else {
            UseCaseResult.Error(IllegalStateException())
        }
    }

    else -> UseCaseResult.Error(error ?: Exception())
}

fun <T> UseCaseResult<T>.asState() = when (this) {
    is UseCaseResult.Loading -> LoadState.loading()
    is UseCaseResult.Success -> LoadState.success(this.data)
    is UseCaseResult.Error -> LoadState.error(this.exception)
}

fun <T, U> UseCaseResult<T>.mapData(mapData: (data: T) -> U): UseCaseResult<U> = when (this) {
    is UseCaseResult.Loading -> UseCaseResult.Loading
    is UseCaseResult.Success -> UseCaseResult.Success(mapData(this.data))
    is UseCaseResult.Error -> UseCaseResult.Error(this.exception, this.messageResId)
}

fun <T> UseCaseResult<T>?.isNetworkError(): Boolean {
    return this is UseCaseResult.Error && (this.exception is NetworkException || this.exception is NoConnectivityException)
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

fun Throwable.asUseCaseResult() = when (this) {
    is ApiException -> {
        UseCaseResult.Error(this)
    }

    is NetworkException,
    is NoConnectivityException,
    is SocketException,
    is UnknownHostException,
    is SSLException,
    is SocketTimeoutException,
    is IOException,
    is retrofit2.HttpException,
    -> {
        UseCaseResult.Error(this, net.maxsmr.core.network.R.string.error_no_internet)
    }

    else -> {
        UseCaseResult.Error(this)
    }
}