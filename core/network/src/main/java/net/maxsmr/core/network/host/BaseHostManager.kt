package net.maxsmr.core.network.host

import java.util.Locale

interface BaseHostManager: HostManager {

    override val baseUrl: String
        get() {
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

    val useHttps: Boolean

    val host: String

    val port: Int?
}