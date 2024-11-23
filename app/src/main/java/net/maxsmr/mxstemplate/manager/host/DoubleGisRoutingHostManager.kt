package net.maxsmr.mxstemplate.manager.host

import net.maxsmr.core.network.HostManager

class DoubleGisRoutingHostManager: HostManager {

    override val useHttps: Boolean = true

    override val host: String = "routing.api.2gis.com"

    override val port: Int? = null
}