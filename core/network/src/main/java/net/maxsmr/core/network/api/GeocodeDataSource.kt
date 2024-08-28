package net.maxsmr.core.network.api

import net.maxsmr.core.domain.entities.feature.address_sorter.AddressGeocode
import net.maxsmr.core.network.api.yandex.geocode.YandexGeocodeDataService
import net.maxsmr.core.network.client.retrofit.YandexGeocodeRetrofitClient

interface GeocodeDataSource {

    suspend fun geocode(
        geocode: String,
    ): AddressGeocode?
}

class YandexGeocodeDataSource(
    private val retrofit: YandexGeocodeRetrofitClient,
) : GeocodeDataSource {

    override suspend fun geocode(geocode: String): AddressGeocode? {
        return YandexGeocodeDataService.instance(retrofit).geocode(geocode).asDomain()
    }
}