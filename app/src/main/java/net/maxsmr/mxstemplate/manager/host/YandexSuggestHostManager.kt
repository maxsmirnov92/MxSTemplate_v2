package net.maxsmr.mxstemplate.manager.host

import net.maxsmr.core.network.HostManager

class YandexSuggestHostManager: HostManager {

    override val useHttps: Boolean = true

    override val host: String = "suggest-maps.yandex.ru"

    override val port: Int? = null
}