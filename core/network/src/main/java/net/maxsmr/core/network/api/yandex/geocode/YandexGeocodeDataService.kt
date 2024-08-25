package net.maxsmr.core.network.api.yandex.geocode

import net.maxsmr.core.network.retrofit.client.YandexGeocodeRetrofitClient
import net.maxsmr.core.network.retrofit.interceptors.Authorization
import retrofit2.http.GET
import retrofit2.http.Query

interface YandexGeocodeDataService {

    /**
     * Позволяет узнать координаты объекта по его адресу или названию, либо в обратную сторону — узнать адрес объекта по его кординатам.
     *
     * @param geocode Обязательный параметр
     *
     * Адрес либо географические координаты искомого объекта. Указанные данные определяют тип геокодирования:
     *
     * Если указан адрес, то он преобразуется в координаты объекта. Этот процесс называется прямым геокодированием.
     * Если указаны координаты, они преобразуются в адрес объекта. Этот процесс называется обратным геокодированием.
     *
     * @param lang Язык ответа и региональные особенности карты.
     *
     * Формат записи lang=language_region, где:
     *
     * language — двузначный код языка. Указывается в формате ISO 639-1. Задает язык, на котором будут отображаться названия географических объектов.
     * region — двузначный код страны. Указывается в формате ISO 3166-1. Определяет региональные особенности.
     */
    @Authorization
    @GET("1.x")
    suspend fun geocode(
        @Query("geocode") geocode: String,
        @Query("lang") lang: String,
    ): GeocodeResponse

    companion object {

        @Volatile
        private var instance: YandexGeocodeDataService? = null

        @JvmStatic
        fun instance(client: YandexGeocodeRetrofitClient): YandexGeocodeDataService =
            instance ?: synchronized(this) {
                instance ?: client.create(YandexGeocodeDataService::class.java).also { instance = it }
            }
    }
}