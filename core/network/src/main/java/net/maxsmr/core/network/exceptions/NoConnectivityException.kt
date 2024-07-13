package net.maxsmr.core.network.exceptions

import net.maxsmr.core.network.NETWORK_OFFLINE
import java.io.IOException

open class NoConnectivityException(
    val code: Int = NETWORK_OFFLINE,
    message: String = ""
) : IOException(message)