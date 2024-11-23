package net.maxsmr.mxstemplate.manager.host

import net.maxsmr.core.network.HostManager

class YandexGeocodeHostManager: HostManager {

    override val useHttps: Boolean = true

    override val host: String = "geocode-maps.yandex.ru"

    override val port: Int? = null
}