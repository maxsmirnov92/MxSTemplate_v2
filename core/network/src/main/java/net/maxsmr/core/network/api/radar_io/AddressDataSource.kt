package net.maxsmr.core.network.api.radar_io

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import net.maxsmr.core.network.api.radar_io.internal.RadarIoDataService
import net.maxsmr.core.network.retrofit.client.RadarIoRetrofitClient

interface AddressDataSource {

    suspend fun suggest(
        query: String,
        latitude: Float? = null,
        longitude: Float? = null,
        country: String = "RU",
    ): List<AddressSuggest>
}

class RadarIoDataSource(
    private val retrofitClient: RadarIoRetrofitClient,
) : AddressDataSource {

    override suspend fun suggest(
        query: String,
        latitude: Float?,
        longitude: Float?,
        country: String,
    ): List<AddressSuggest> = withContext(Dispatchers.IO) {
        val near =
            if (latitude != null && longitude != null) {
                "$latitude,$longitude"
            } else {
                null
            }
        RadarIoDataService.instance(retrofitClient).suggest(query, near, country).asDomain()
    }
}

