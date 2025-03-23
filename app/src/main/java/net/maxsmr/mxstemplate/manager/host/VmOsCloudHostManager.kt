package net.maxsmr.mxstemplate.manager.host

import net.maxsmr.core.network.host.BaseHostManager
import net.maxsmr.core.network.host.HostManager

class VmOsCloudHostManager: BaseHostManager {

    override val useHttps: Boolean = true

    override val host: String = "api.vmoscloud.com"

    override val port: Int? = null
}