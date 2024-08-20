package net.maxsmr.mxstemplate.manager.host

import net.maxsmr.core.network.retrofit.interceptors.HostChangeListener
import net.maxsmr.core.network.retrofit.interceptors.HostManager

class RadarIoHostManager: HostManager {

    override var hostChangeListener: HostChangeListener? = null

    override fun useHttps(): Boolean = true

    override fun getHost(): String = "api.radar.io"

    override fun getPort(): String? = null
}