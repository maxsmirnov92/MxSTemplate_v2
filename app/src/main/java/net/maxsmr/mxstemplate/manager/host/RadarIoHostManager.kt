package net.maxsmr.mxstemplate.manager.host

import net.maxsmr.core.network.HostManager

class RadarIoHostManager: HostManager {

    override val useHttps: Boolean = true

    override val host: String = "api.radar.io"

    override val port: Int? = null
}