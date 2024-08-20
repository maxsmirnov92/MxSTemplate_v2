package net.maxsmr.core.network.api.yandex

import net.maxsmr.core.network.retrofit.client.CommonRetrofitClient
import net.maxsmr.core.network.retrofit.converters.ResponseObjectType
import net.maxsmr.core.network.retrofit.interceptors.Authorization
import retrofit2.http.GET
import retrofit2.http.Query

internal interface YandexSuggestDataService {

//    @Authorization
//    @GET("1.x")
//    suspend fun geocode(
//        @Query("geocode") geocode: String,
//        @Query("lang") lang: String = "RU"
//    ): GeocodeResponse

    /**
     * @param text Пользовательский ввод (префикс). Непустая строка в кодировке UTF-8.
     * @param location Координаты центра окна поиска, если оно задается в виде прямоугольника с координатами центра и размерами.
     * Используется формат {lon},{lat}
     */
    @Authorization
    @GET("v1/suggest")
    @ResponseObjectType(SuggestResponse::class)
    suspend fun suggest(
        @Query("text") text: String,
        @Query("ll") location: String? = null,
        @Query("lang") lang: String,
    ): SuggestResponse

    companion object {

        @Volatile
        private var instance: YandexSuggestDataService? = null

        @JvmStatic
        fun instance(client: CommonRetrofitClient): YandexSuggestDataService =
            instance ?: synchronized(this) {
                instance ?: client.create(YandexSuggestDataService::class.java).also { instance = it }
            }
    }
}