package net.maxsmr.core.network.api

import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import net.maxsmr.core.network.api.radar_io.RadarIoDataService
import net.maxsmr.core.network.api.yandex.suggest.YandexSuggestDataService
import net.maxsmr.core.network.client.retrofit.CommonRetrofitClient

interface SuggestDataSource {

    suspend fun suggest(
        query: String,
        location: Address.Location? = null,
    ): List<AddressSuggest>
}

class RadarIoSuggestDataSource(
    private val retrofit: CommonRetrofitClient,
) : SuggestDataSource {

    override suspend fun suggest(
        query: String,
        location: Address.Location?,
    ): List<AddressSuggest> {
        val locationText = location?.let {
            "${it.latitude},${it.longitude}"
        }
        return RadarIoDataService.instance(retrofit).suggest(query, locationText).asDomain()
    }
}

class YandexSuggestDataSource(
    private val retrofit: CommonRetrofitClient,
) : SuggestDataSource {

    override suspend fun suggest(
        query: String,
        location: Address.Location?,
    ): List<AddressSuggest> {
        val locationText = location?.let {
            "${it.longitude},${it.latitude}"
        }
        return YandexSuggestDataService.instance(retrofit).suggest(query, locationText).asDomain()
    }
}

