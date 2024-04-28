package net.maxsmr.core.network.exceptions

open class ApiException(
    val code: Int,
    override val message: String = "",
) : RuntimeException(message)