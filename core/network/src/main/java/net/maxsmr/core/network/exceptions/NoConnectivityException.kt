package net.maxsmr.core.network.exceptions

import android.content.Context
import net.maxsmr.core.network.CustomErrorCode
import net.maxsmr.core.network.R

open class NoConnectivityException(message: String) : NetworkException(
    CustomErrorCode.NETWORK_OFFLINE.code,
    message
) {

    constructor(context: Context) : this(
        context.getString(R.string.error_no_connection)
    )
}