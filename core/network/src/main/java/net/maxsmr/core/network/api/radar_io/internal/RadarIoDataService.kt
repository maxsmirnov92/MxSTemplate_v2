package net.maxsmr.core.network.api.radar_io.internal

import net.maxsmr.core.network.retrofit.client.RadarIoRetrofitClient
import net.maxsmr.core.network.retrofit.converters.RadarIoResponseObjectType
import net.maxsmr.core.network.retrofit.interceptors.ServiceFields
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryName

internal interface RadarIoDataService {

    @ServiceFields
    @RadarIoResponseObjectType(AutocompleteResponse::class)
    @GET("v1/search/autocomplete")
    suspend fun suggest(
        @Query("query") query: String,
        @Query("near") near: String? = null,
        @Query("country") country: String = "RU"
    ): AutocompleteResponse

    companion object {

        @Volatile
        private var instance: RadarIoDataService? = null

        @JvmStatic
        fun instance(retrofitClient: RadarIoRetrofitClient): RadarIoDataService =
            instance ?: synchronized(this) {
                instance ?: retrofitClient.create(RadarIoDataService::class.java).also { instance = it }
            }
    }
}