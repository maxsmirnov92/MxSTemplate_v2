package net.maxsmr.core.network.exceptions

import android.content.Context

class NoPreferableConnectivityException(
    val types: Set<PreferableType>,
    context: Context
): NoConnectivityException(context.getMessage(types)) {

    enum class PreferableType {
        CELLULAR, WIFI
    }

    companion object {

        private fun Context.getMessage(types: Set<PreferableType>): String {
            val typeNames = types.map {
                when (it) {
                    PreferableType.CELLULAR -> getString(net.maxsmr.core.network.R.string.network_type_cellular)
                    PreferableType.WIFI -> getString(net.maxsmr.core.network.R.string.network_type_wifi)
                }
            }
            return if (typeNames.isNotEmpty()) {
                getString(
                    net.maxsmr.core.network.R.string.error_no_preferable_connection_format,
                    typeNames.joinToString("/")
                )
            } else {
                getString(
                    net.maxsmr.core.network.R.string.error_no_preferable_connection
                )
            }
        }
    }
}