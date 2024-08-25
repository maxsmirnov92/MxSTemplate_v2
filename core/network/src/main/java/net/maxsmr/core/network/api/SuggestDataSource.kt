package net.maxsmr.core.network.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import net.maxsmr.core.network.api.radar_io.RadarIoDataService
import net.maxsmr.core.network.api.yandex.suggest.YandexSuggestDataService
import net.maxsmr.core.network.retrofit.client.CommonRetrofitClient

interface SuggestDataSource {

    suspend fun suggest(
        query: String,
        latitude: Float? = null,
        longitude: Float? = null,
        country: String = "RU",
        lang: String = "ru",
    ): List<AddressSuggest>
}

class RadarIoSuggestDataSource(
    private val retrofit: CommonRetrofitClient,
) : SuggestDataSource {

    override suspend fun suggest(
        query: String,
        latitude: Float?,
        longitude: Float?,
        country: String,
        lang: String,
    ): List<AddressSuggest> = withContext(Dispatchers.IO) {
        val near = if (latitude != null && longitude != null) {
            "$latitude,$longitude"
        } else {
            null
        }
        RadarIoDataService.instance(retrofit).suggest(query, near, country).asDomain()
    }
}

class YandexSuggestDataSource(
    private val retrofit: CommonRetrofitClient,
) : SuggestDataSource {

    override suspend fun suggest(
        query: String,
        latitude: Float?,
        longitude: Float?,
        country: String,
        lang: String,
    ): List<AddressSuggest> = withContext(Dispatchers.IO) {
        val location = if (latitude != null && longitude != null) {
            "$longitude,$latitude"
        } else {
            null
        }
        YandexSuggestDataService.instance(retrofit).suggest(query, location, lang).asDomain()
    }
}

