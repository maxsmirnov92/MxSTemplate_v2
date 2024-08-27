package net.maxsmr.core.android.network

import net.maxsmr.core.network.client.okhttp.interceptors.ConnectivityChecker

object NetworkConnectivityChecker: ConnectivityChecker {

    override fun isConnected(): Boolean = NetworkStateManager.hasConnection()
}