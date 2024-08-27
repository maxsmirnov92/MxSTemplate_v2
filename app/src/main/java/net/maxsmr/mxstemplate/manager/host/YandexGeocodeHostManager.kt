package net.maxsmr.mxstemplate.manager.host

import net.maxsmr.core.network.HostChangeListener
import net.maxsmr.core.network.HostManager

class YandexGeocodeHostManager: HostManager {

    override var hostChangeListener: HostChangeListener? = null

    override fun useHttps(): Boolean = true

    override fun getHost(): String = "geocode-maps.yandex.ru"

    override fun getPort(): String? = null
}