package net.maxsmr.address_sorter.manager.host

import net.maxsmr.core.network.HostChangeListener
import net.maxsmr.core.network.HostManager

class YandexSuggestHostManager: HostManager {

    override var hostChangeListener: HostChangeListener? = null

    override fun useHttps(): Boolean = true

    override fun getHost(): String = "suggest-maps.yandex.ru"

    override fun getPort(): String? = null
}