package net.maxsmr.core.network.api

import net.maxsmr.core.domain.entities.feature.address_sorter.AddressGeocode
import net.maxsmr.core.network.api.yandex.geocode.YandexGeocodeDataService
import net.maxsmr.core.network.retrofit.client.YandexGeocodeRetrofitClient

interface GeocodeDataSource {

    suspend fun geocode(
        geocode: String,
        lang: String = "ru_RU",
    ): AddressGeocode?
}

class YandexGeocodeDataSource(
    private val retrofit: YandexGeocodeRetrofitClient,
) : GeocodeDataSource {

    override suspend fun geocode(geocode: String, lang: String): AddressGeocode? {
        return YandexGeocodeDataService.instance(retrofit).geocode(geocode, lang).asDomain()
    }
}