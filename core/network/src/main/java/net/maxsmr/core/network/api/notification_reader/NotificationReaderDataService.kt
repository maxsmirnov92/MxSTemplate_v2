package net.maxsmr.core.network.api.notification_reader

import net.maxsmr.core.network.client.okhttp.interceptors.Authorization
import net.maxsmr.core.network.client.retrofit.CommonRetrofitClient
import net.maxsmr.core.network.retrofit.converters.ResponseObjectType
import retrofit2.http.Body
import retrofit2.http.POST

interface NotificationReaderDataService {

    @Authorization
    @POST("/")
    @ResponseObjectType(NotificationReaderDataResponse::class)
    suspend fun notifyData(
        @Body request: List<NotificationReaderDataRequest>,
    ): NotificationReaderDataResponse

    companion object {

        @Volatile
        private var instance: NotificationReaderDataService? = null

        @JvmStatic
        fun instance(client: CommonRetrofitClient): NotificationReaderDataService =
            instance ?: synchronized(this) {
                instance ?: client.create(NotificationReaderDataService::class.java).also { instance = it }
            }
    }
}