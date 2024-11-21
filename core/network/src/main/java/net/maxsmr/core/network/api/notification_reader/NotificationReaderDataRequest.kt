package net.maxsmr.core.network.api.notification_reader

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class NotificationReaderDataRequest(
    val contentText: String,
    val packageName: String,
    val timestamp: Instant,
) {

    constructor(
        content: String,
        packageName: String,
        timestamp: Long,
    ) : this(content, packageName, Instant.fromEpochMilliseconds(timestamp))
}