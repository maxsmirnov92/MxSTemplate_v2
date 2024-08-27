package net.maxsmr.core.network

interface HostManager {

    var hostChangeListener: HostChangeListener?

    fun useHttps(): Boolean

    fun getHost(): String

    fun getPort(): String?

    fun getBaseUrl(): String {
        val port = getPort()
        return if (port != null) {
            String.format(
                "%s://%s:%s",
                if (useHttps()) "https" else "http", getHost(), port
            )
        } else {
            String.format(
                "%s://%s",
                if (useHttps()) "https" else "http", getHost()
            )
        }
    }

}

interface HostChangeListener {

    fun onHostChanged(url: String)
}