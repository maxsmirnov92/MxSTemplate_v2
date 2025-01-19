package net.maxsmr.mxstemplate.manager.host

import net.maxsmr.core.network.host.BaseHostManager

class VkHostManager: BaseHostManager {

    override val useHttps: Boolean = true

    override val host: String = "api.vk.com"

    override val port: Int? = null
}