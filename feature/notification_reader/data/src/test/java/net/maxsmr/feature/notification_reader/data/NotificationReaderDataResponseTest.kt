package net.maxsmr.feature.notification_reader.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.maxsmr.core.network.api.notification_reader.NotificationReaderDataResponse
import org.junit.Test
import kotlin.test.assertEquals

class NotificationReaderDataResponseTest {

    @Test
    fun testSerializer() {
        assertEquals(NotificationReaderDataResponse, Json.decodeFromString<NotificationReaderDataResponse>(""))
        assertEquals(NotificationReaderDataResponse, Json.decodeFromString<NotificationReaderDataResponse>("{}"))
        assertEquals("{}", Json.encodeToString(NotificationReaderDataResponse))
    }
}