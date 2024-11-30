package net.maxsmr.mxstemplate.manager.host

import net.maxsmr.core.network.host.BaseHostManager
import net.maxsmr.core.network.host.HostManager

class DoubleGisRoutingHostManager: BaseHostManager {

    override val useHttps: Boolean = true

    override val host: String = "routing.api.2gis.com"

    override val port: Int? = null
}