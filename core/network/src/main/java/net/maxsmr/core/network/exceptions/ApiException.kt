package net.maxsmr.core.network.exceptions

import net.maxsmr.commonutils.text.EMPTY_STRING

open class ApiException(
    val code: Int,
    message: String = EMPTY_STRING,
) : RuntimeException(message)