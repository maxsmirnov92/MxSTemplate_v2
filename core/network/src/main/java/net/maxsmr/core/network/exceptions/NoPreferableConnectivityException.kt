package net.maxsmr.core.network.exceptions

class NoPreferableConnectivityException(val types: Set<PreferableType>): NoConnectivityException() {

    enum class PreferableType {
        CELLULAR, WIFI
    }
}