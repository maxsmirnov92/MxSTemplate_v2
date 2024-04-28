package net.maxsmr.core.network

import net.maxsmr.core.network.exceptions.ApiException
import net.maxsmr.core.network.exceptions.NetworkException
import net.maxsmr.core.network.exceptions.NoConnectivityException

const val UNKNOWN_ERROR = -1
const val NO_ERROR = 0
const val SUCCESS = 200

const val UNAUTHORIZED = 401
const val FORBIDDEN = 403

/**
 * Отсутствие сетевого ответа (оффлайн, от сервера не пришло никакого кода)
 */
const val NETWORK_OFFLINE = 600

fun Throwable.getErrorCode(): Int {
    return when (this) {
        is NetworkException -> {
            code
        }
        is ApiException -> {
            code
        }
        is NoConnectivityException -> {
            code
        }
        else -> UNKNOWN_ERROR
    }
}