package net.maxsmr.feature.download.data.manager

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.maxsmr.core.android.network.NetworkStateManager
import net.maxsmr.core.domain.entities.feature.settings.AppSettings
import net.maxsmr.core.network.exceptions.NoConnectivityException
import net.maxsmr.core.network.exceptions.NoPreferableConnectivityException
import net.maxsmr.core.network.exceptions.NoPreferableConnectivityException.PreferableType
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlin.jvm.Throws

fun CoroutineScope.observeNetworkStateWithSettings(
    repo: SettingsDataStoreRepository,
    collectFunc: suspend (NetworkStateWithSettings) -> Unit,
): Job {
    return launch {
        combine(
            NetworkStateManager.asFlow(this),
            repo.settingsFlow
        ) { connectionInfo: NetworkStateManager.ConnectionInfo, settings: AppSettings ->
            NetworkStateWithSettings(connectionInfo, settings.retryDownloads, settings.loadByWiFiOnly)
        }.collectLatest {
            collectFunc(it)
        }
    }
}

@Throws(NoPreferableConnectivityException::class)
fun Context.checkPreferableConnection(preferredConnectionTypes: Set<PreferableType>) {
    var hasPreferableConnection = true
    val connectionInfo = NetworkStateManager.getConnectionInfo()
    if (connectionInfo.has &&
            (connectionInfo.hasWiFi != null || connectionInfo.hasCellular != null)
            && preferredConnectionTypes.isNotEmpty()
    ) {
        // предпочтительные типы указаны и в API информация возвращается
        hasPreferableConnection = false
        run breaking@{
            preferredConnectionTypes.forEach {
                when (it) {
                    PreferableType.CELLULAR -> if (connectionInfo.hasCellular == true) {
                        hasPreferableConnection = true
                        return@breaking
                    }

                    PreferableType.WIFI -> if (connectionInfo.hasWiFi == true) {
                        hasPreferableConnection = true
                        return@breaking
                    }
                }
            }
        }
    }
    if (!hasPreferableConnection) {
        throw NoPreferableConnectivityException(preferredConnectionTypes, this)
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
                    // поиск зафейленных загрузок по причине отсутствия WiFi, если это соединение появилось
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
}