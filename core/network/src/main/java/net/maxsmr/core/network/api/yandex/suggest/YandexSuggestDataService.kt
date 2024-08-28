package net.maxsmr.core.network.api.yandex.suggest

import net.maxsmr.core.network.client.retrofit.CommonRetrofitClient
import net.maxsmr.core.network.retrofit.converters.ResponseObjectType
import net.maxsmr.core.network.client.okhttp.interceptors.Authorization
import retrofit2.http.GET
import retrofit2.http.Query

internal interface YandexSuggestDataService {

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