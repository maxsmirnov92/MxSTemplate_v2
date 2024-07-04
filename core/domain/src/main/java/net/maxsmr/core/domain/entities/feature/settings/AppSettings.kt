package net.maxsmr.core.domain.entities.feature.settings

import java.io.Serializable

@kotlinx.serialization.Serializable
data class AppSettings(
    val maxDownloads: Int = 3,
    val connectTimeout: Long = 30,
    val loadByWiFiOnly: Boolean = false,
    val retryOnConnectionFailure: Boolean = true,
    val retryDownloads: Boolean = true,
    val disableNotifications: Boolean = false,
    val updateNotificationInterval: Long = UPDATE_NOTIFICATION_INTERVAL_DEFAULT,
    val openLinksInExternalApps: Boolean = true,
    val startPageUrl: String = "https://google.com",
) : Serializable {

    companion object {

        // если дефолтное значение поля используется где-либо,
        // выносить в константу

        const val MAX_DOWNLOADS_UNLIMITED = 0

        const val UPDATE_NOTIFICATION_INTERVAL_MIN = 100L
        const val UPDATE_NOTIFICATION_INTERVAL_DEFAULT = 300L
    }
}