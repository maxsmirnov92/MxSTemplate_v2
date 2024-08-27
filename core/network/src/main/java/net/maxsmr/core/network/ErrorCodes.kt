package net.maxsmr.core.network

import net.maxsmr.core.network.exceptions.ApiException
import net.maxsmr.core.network.exceptions.NetworkException
import net.maxsmr.core.network.exceptions.NoConnectivityException

const val UNKNOWN_ERROR = -1
const val NO_ERROR_API = 0

/**
 * Отсутствие сетевого ответа (оффлайн, от сервера не пришло никакого кода)
 */
const val NETWORK_OFFLINE = 600

/**
 * Ошибка разбора JSON в ответе
 */
const val JSON_PARSE_ERROR = 601

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

enum class ApiErrorCode(val code: Int) {

    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    BAD_GATEWAY(502),
    SERVICE_TEMPORARILY_UNAVAILABLE(503),
}