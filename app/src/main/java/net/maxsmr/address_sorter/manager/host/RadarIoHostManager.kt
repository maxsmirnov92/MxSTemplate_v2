package net.maxsmr.address_sorter.manager.host

import net.maxsmr.core.network.HostChangeListener
import net.maxsmr.core.network.HostManager

class RadarIoHostManager: HostManager {

    override var hostChangeListener: HostChangeListener? = null

    override fun useHttps(): Boolean = true

    override fun getHost(): String = "api.radar.io"

    override fun getPort(): String? = null
}