package net.maxsmr.core.network.exceptions

import android.content.Context
import net.maxsmr.core.network.NETWORK_OFFLINE
import net.maxsmr.core.network.R

open class NoConnectivityException(message: String) : NetworkException(
    code = NETWORK_OFFLINE,
    message = message
) {

    constructor(context: Context) : this(
        context.getString(R.string.error_no_connection)
    )
}