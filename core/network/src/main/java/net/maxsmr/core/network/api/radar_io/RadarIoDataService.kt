package net.maxsmr.core.network.api.radar_io

import net.maxsmr.core.network.client.retrofit.CommonRetrofitClient
import net.maxsmr.core.network.retrofit.converters.ResponseObjectType
import net.maxsmr.core.network.client.okhttp.interceptors.Authorization
import retrofit2.http.GET
import retrofit2.http.Query

internal interface RadarIoDataService {

    @Authorization
    @GET("v1/search/autocomplete")
    @ResponseObjectType(SuggestResponse::class)
    suspend fun suggest(
        @Query("query") query: String,
        @Query("near") near: String? = null,
    ): SuggestResponse

    companion object {

        @Volatile
        private var instance: RadarIoDataService? = null

        @JvmStatic
        fun instance(client: CommonRetrofitClient): RadarIoDataService =
            instance ?: synchronized(this) {
                instance ?: client.create(RadarIoDataService::class.java).also { instance = it }
            }
    }
}