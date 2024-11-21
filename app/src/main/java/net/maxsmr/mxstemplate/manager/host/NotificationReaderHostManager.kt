package net.maxsmr.mxstemplate.manager.host

import net.maxsmr.core.network.host.BaseHostManager

class NotificationReaderHostManager(
    override val host: String,
    override val useHttps: Boolean = true,
    override val port: Int? = null
): BaseHostManager