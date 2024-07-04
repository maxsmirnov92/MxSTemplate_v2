package net.maxsmr.core.network

import net.maxsmr.core.network.exceptions.ApiException
import net.maxsmr.core.network.exceptions.NetworkException

const val UNKNOWN_ERROR = -1
const val NO_ERROR_API = 0

fun Throwable.getErrorCode(): Int {
    return when (this) {
        is NetworkException -> {
            code
        }
        is ApiException -> {
            code
        }
        else -> UNKNOWN_ERROR
    }
}

enum class HttpErrorCode(val code: Int) {

    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    BAD_GATEWAY(502),
    SERVICE_TEMPORARILY_UNAVAILABLE(503);

    companion object {

        @JvmStatic
        fun resolve(code: Int) = entries.find { it.code == code }
    }
}

enum class CustomErrorCode(val code: Int) {

    NETWORK_OFFLINE(600)
}