package net.maxsmr.feature.preferences.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import net.maxsmr.core.android.network.NetworkStateManager
import net.maxsmr.core.domain.entities.feature.settings.AppSettings
import net.maxsmr.core.network.exceptions.NoConnectivityException
import net.maxsmr.core.network.exceptions.NoPreferableConnectivityException
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import java.net.SocketException
import java.net.SocketTimeoutException

fun SettingsDataStoreRepository.combineNetworkStateWithSettings(networkStateManager: NetworkStateManager): Flow<NetworkStateWithSettings> {
    return combine(
        networkStateManager.asFlow(),
        settingsFlow
    ) { connectionInfo: NetworkStateManager.ConnectionInfo, settings: AppSettings ->
        NetworkStateWithSettings(connectionInfo, settings.retryDownloads, settings.loadByWiFiOnly)
    }
}

data class NetworkStateWithSettings(
    val connectionInfo: NetworkStateManager.ConnectionInfo,
    val shouldRetry: Boolean,
    val loadByWiFiOnly: Boolean,
) {

    fun shouldReload(error: Throwable?): Boolean {
        if (!shouldRetry) return false

        return when (error) {

            is NoConnectivityException, is SocketException, is SocketTimeoutException -> {
                if (loadByWiFiOnly && error is NoPreferableConnectivityException) {
                    // требуется по причине отсутствия Wi-Fi, если это соединение появилось
                    connectionInfo.hasWiFi == true
                } else {
                    // или по причине любой сети, если она появилась
                    connectionInfo.has
                }
            }

            else -> {
                false
            }
        }
    }

    override fun toString(): String {
        return "NetworkStateWithSettings(connectionInfo=$connectionInfo, " +
                "shouldRetry=$shouldRetry, " +
                "loadByWiFiOnly=$loadByWiFiOnly)"
    }
}