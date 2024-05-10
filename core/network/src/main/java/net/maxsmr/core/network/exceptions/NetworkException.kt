package net.maxsmr.core.network.exceptions

import net.maxsmr.core.network.UNKNOWN_ERROR

open class NetworkException(
    val code: Int = UNKNOWN_ERROR,
    message: String? = null,
) : RuntimeException(message)