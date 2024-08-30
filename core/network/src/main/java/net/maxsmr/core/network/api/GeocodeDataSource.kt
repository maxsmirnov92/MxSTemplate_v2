package net.maxsmr.core.network.api

import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressGeocode
import net.maxsmr.core.network.api.yandex.geocode.YandexGeocodeDataService
import net.maxsmr.core.network.client.retrofit.YandexGeocodeRetrofitClient

interface GeocodeDataSource {

    /**
     * @param getDistanceFunc null, если последнее местоположение неизвестно
     */
    suspend fun directGeocode(
        geocode: String,
        getDistanceFunc: ((Address.Location) -> Float?)?,
    ): AddressGeocode?

    suspend fun reverseGeocode(
        location: Address.Location
    ): AddressGeocode?
}

class YandexGeocodeDataSource(
    private val retrofit: YandexGeocodeRetrofitClient,
) : GeocodeDataSource {

    override suspend fun directGeocode(
        geocode: String,
        getDistanceFunc: ((Address.Location) -> Float?)?,
    ): AddressGeocode? {
        return YandexGeocodeDataService.instance(retrofit).geocode(geocode).asDirectGeocodeDomain(getDistanceFunc)
    }

    override suspend fun reverseGeocode(location: Address.Location): AddressGeocode? {
        return YandexGeocodeDataService.instance(retrofit).geocode(location.asReadableString(false)).asReverseGeocodeDomain(location)
    }
}