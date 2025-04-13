package net.maxsmr.core.network.api.notification_reader

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class NotificationReaderDataRequest(
    val notifications: List<NotificationReaderData>
) {

    @Serializable
    data class NotificationReaderData(
        val id: Long,
        val contentText: String,
        val packageName: String,
        val appName: String,
        val timestamp: Instant,
    ) {

        constructor(
            id: Long,
            content: String,
            packageName: String,
            appName: String,
            timestamp: Long,
        ) : this(id, content, packageName, appName, Instant.fromEpochMilliseconds(timestamp))
    }
}