package net.maxsmr.core.network.api.doublegis

import net.maxsmr.core.network.client.okhttp.interceptors.Authorization
import net.maxsmr.core.network.client.retrofit.CommonRetrofitClient
import net.maxsmr.core.network.retrofit.converters.ResponseObjectType
import retrofit2.http.Body
import retrofit2.http.POST

interface DoubleGisRoutingDataService {

    /**
     * Distance Matrix API позволяет получить информацию о расстоянии и времени в пути между точками на карте.
     */
    @Authorization
    @POST("get_dist_matrix")
    @ResponseObjectType(RoutingResponse::class)
    suspend fun getDistanceMatrix(
        @Body request: RoutingRequest,
    ): RoutingResponse

    companion object {

        @Volatile
        private var instance: DoubleGisRoutingDataService? = null

        @JvmStatic
        fun instance(client: CommonRetrofitClient): DoubleGisRoutingDataService =
            instance ?: synchronized(this) {
                instance ?: client.create(DoubleGisRoutingDataService::class.java).also { instance = it }
            }
    }
}