package net.maxsmr.core.android.network

import net.maxsmr.core.network.client.okhttp.interceptors.ConnectivityChecker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkConnectivityChecker @Inject constructor(
    private val manager: NetworkStateManager
): ConnectivityChecker {

    override fun isConnected(): Boolean = manager.hasConnection()
}