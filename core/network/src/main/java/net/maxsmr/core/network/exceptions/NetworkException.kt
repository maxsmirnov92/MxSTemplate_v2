package net.maxsmr.core.network.exceptions

import net.maxsmr.core.network.UNKNOWN_ERROR

open class NetworkException(
    val code: Int = UNKNOWN_ERROR,
    cause: Throwable? = null,
    message: String? = cause?.message,
) : RuntimeException(message, cause)