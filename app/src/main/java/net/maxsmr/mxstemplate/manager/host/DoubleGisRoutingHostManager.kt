package net.maxsmr.mxstemplate.manager.host

import net.maxsmr.core.network.HostChangeListener
import net.maxsmr.core.network.HostManager

class DoubleGisRoutingHostManager: HostManager {

    override var hostChangeListener: HostChangeListener? = null

    override fun useHttps(): Boolean = true

    override fun getHost(): String = "routing.api.2gis.com"

    override fun getPort(): String? = null
}