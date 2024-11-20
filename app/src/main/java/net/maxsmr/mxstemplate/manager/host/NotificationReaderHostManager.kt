package net.maxsmr.mxstemplate.manager.host

import net.maxsmr.core.network.host.BaseHostManager

class NotificationReaderHostManager: BaseHostManager {

    override val useHttps: Boolean = true

    override val host: String = "192.168.1.244"

    override val port: Int? = null
}