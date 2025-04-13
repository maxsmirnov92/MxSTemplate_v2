package net.maxsmr.core.network.api

import kotlinx.coroutines.delay
import net.maxsmr.core.network.UNKNOWN_ERROR
import net.maxsmr.core.network.api.notification_reader.NotificationReaderDataRequest
import net.maxsmr.core.network.api.notification_reader.NotificationReaderDataRequest.NotificationReaderData
import net.maxsmr.core.network.api.notification_reader.NotificationReaderDataService
import net.maxsmr.core.network.client.retrofit.CommonRetrofitClient
import net.maxsmr.core.network.exceptions.ApiException
import kotlin.random.Random

interface BaseNotificationReaderDataSource {

    suspend fun notifyData(notifications: List<NotificationReaderData>)
}

class NotificationReaderDataSource(
    private val retrofit: CommonRetrofitClient,
) : BaseNotificationReaderDataSource {

    override suspend fun notifyData(notifications: List<NotificationReaderData>) {
        NotificationReaderDataService.instance(retrofit).notifyData(NotificationReaderDataRequest(notifications))
    }
}

class MockNotificationReaderDataSource : BaseNotificationReaderDataSource {

    override suspend fun notifyData(notifications: List<NotificationReaderData>) {
        delay(5000)
        if (Random.nextInt(Int.MAX_VALUE / 2) % 2 != 0) {
            throw ApiException(UNKNOWN_ERROR, "Random error")
        }
    }
}