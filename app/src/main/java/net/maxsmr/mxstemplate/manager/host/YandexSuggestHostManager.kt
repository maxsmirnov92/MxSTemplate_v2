package net.maxsmr.mxstemplate.manager.host

import net.maxsmr.core.network.host.BaseHostManager
import net.maxsmr.core.network.host.HostManager

class YandexSuggestHostManager: BaseHostManager {

    override val useHttps: Boolean = true

    override val host: String = "suggest-maps.yandex.ru"

    override val port: Int? = null
}