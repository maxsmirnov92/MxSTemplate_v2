package net.maxsmr.core.domain.entities.feature.settings

import net.maxsmr.core.domain.entities.feature.address_sorter.SortPriority
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.RoutingApp
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.RoutingMode
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.RoutingType
import java.io.Serializable

@kotlinx.serialization.Serializable
data class AppSettings(
    val maxDownloads: Int = 3,
    val connectTimeout: Long = 15,
    val loadByWiFiOnly: Boolean = false,
    val retryOnConnectionFailure: Boolean = true,
    val retryDownloads: Boolean = true,
    val disableNotifications: Boolean = true,
    val updateNotificationInterval: Long = UPDATE_NOTIFICATION_INTERVAL_DEFAULT,
    val openLinksInExternalApps: Boolean = true,
    val startPageUrl: String = "https://google.com",
    val routingMode: RoutingMode = RoutingMode.DOUBLEGIS_DRIVING,
    val routingType: RoutingType = RoutingType.JAM,
    val sortPriority: SortPriority = SortPriority.DISTANCE,
    val routingApp: RoutingApp = RoutingApp.DOUBLEGIS,
    val routingAppFromCurrent: Boolean = true,
    val whiteBlackListPackagesUrl: String = "",
    val isWhiteListPackages: Boolean = true,
    val failedNotificationsWatcherInterval: Long = 15000L
) : Serializable {

    companion object {

        // если дефолтное значение поля используется где-либо,
        // выносить в константу

        const val MAX_DOWNLOADS_UNLIMITED = 0

        const val UPDATE_NOTIFICATION_INTERVAL_MIN = 100L
        const val UPDATE_NOTIFICATION_INTERVAL_DEFAULT = 300L
    }
}