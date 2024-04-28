package net.maxsmr.core.network.api.radar_io.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.Address.Location
import net.maxsmr.core.network.retrofit.converters.BaseRadarIoResponse

@Serializable
class AutocompleteResponse(
    override val meta: Meta,
    private val addresses: List<Address>,
) : BaseRadarIoResponse() {

    @Serializable
    data class Address(
        val latitude: Float,
        val longitude: Float,
        val geometry: Geometry,
        val country: String,
        val countryCode: String,
        val countryFlag: String,
        val county: String,
        val distance: Int,
        val confidence: Confidence,
        val city: String,
        val stateCode: String,
        val state: String,
        val street: String,
        val layer: String,
        val formattedAddress: String,
        val addressLabel: String,
    ) {

        @Serializable
        data class Geometry(
            val type: Type,
            val coordinates: List<Float>,

            ) {

            enum class Type {

                @SerialName("Point")
                POINT
            }
        }

        enum class Confidence {

            @SerialName("exact")
            EXACT
        }

        fun asDomain() = net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest(
            Location(latitude, longitude),
            county.takeIf { it.isNotEmpty() }?.let {
                "$it, $addressLabel"
            } ?: addressLabel,
            distance
        )
    }

    fun asDomain() = addresses.map { it.asDomain() }
}