package net.maxsmr.core.network

import java.util.Locale

interface HostManager {

    val useHttps: Boolean

    val host: String

    val port: Int?

    fun getBaseUrl(): String {
        val port = port
        return if (port != null) {
            String.format(
                Locale.getDefault(),
                "%s://%s:%d",
                if (useHttps) "https" else "http", host, port
            )
        } else {
            String.format(
                "%s://%s",
                if (useHttps) "https" else "http", host
            )
        }
    }
}