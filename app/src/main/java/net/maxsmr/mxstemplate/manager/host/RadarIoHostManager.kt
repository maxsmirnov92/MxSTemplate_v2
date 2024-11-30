package net.maxsmr.mxstemplate.manager.host

import net.maxsmr.core.network.host.BaseHostManager
import net.maxsmr.core.network.host.HostManager

class RadarIoHostManager: BaseHostManager {

    override val useHttps: Boolean = true

    override val host: String = "api.radar.io"

    override val port: Int? = null
}