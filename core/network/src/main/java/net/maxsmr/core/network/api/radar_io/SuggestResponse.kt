package net.maxsmr.core.network.api.radar_io

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.core.domain.entities.feature.address_sorter.Address.Location
import net.maxsmr.core.network.retrofit.converters.api.BaseRadarIoResponse

@Serializable
class SuggestResponse(
    private val addresses: List<Address>,
) : BaseRadarIoResponse() {

    fun asDomain() = addresses.map { it.asDomain() }

    @Serializable
    data class Address(
        val latitude: Float,
        val longitude: Float,
        val geometry: Geometry,
        val country: String,
        val countryCode: String,
        val countryFlag: String,
        val county: String? = null,
        val distance: Float? = null,
        val confidence: String? = null,
        val city: String? = null,
        val number: String? = null,
        val postalCode: String? = null,
        val stateCode: String,
        val state: String? = null,
        val street: String? = null,
        val layer: String? = null,
        val formattedAddress: String? = null,
        val addressLabel: String,
    ) {

        fun asDomain() = net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest(
            county.takeIf { !it.isNullOrEmpty() }?.let {
                "$it, $addressLabel"
            } ?: addressLabel,
            Location(latitude, longitude),
            distance?.takeIf { it >= 0 }
        )

        @Serializable
        data class Geometry(
            val type: String,
            val coordinates: List<Float>,
        )
    }
}