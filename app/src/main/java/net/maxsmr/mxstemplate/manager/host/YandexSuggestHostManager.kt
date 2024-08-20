package net.maxsmr.mxstemplate.manager.host

import net.maxsmr.core.network.retrofit.interceptors.HostChangeListener
import net.maxsmr.core.network.retrofit.interceptors.HostManager

class YandexSuggestHostManager: HostManager {

    override var hostChangeListener: HostChangeListener? = null

    override fun useHttps(): Boolean = true

    override fun getHost(): String = "suggest-maps.yandex.ru"

    override fun getPort(): String? = null
}