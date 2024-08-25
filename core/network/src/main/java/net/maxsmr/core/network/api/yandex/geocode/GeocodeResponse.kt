package net.maxsmr.core.network.api.yandex.geocode

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressGeocode

@Serializable
class GeocodeResponse(
    @SerialName("GeoObjectCollection")
    val collection: GeoObjectCollection,
) {

    fun asDomain(): AddressGeocode? {

        fun String?.toLocation(): Address.Location? {
            val pos = this?.split(" ").orEmpty()
            val latitude = pos.getOrNull(1)?.toFloatOrNull()
            val longitude = pos.getOrNull(0)?.toFloatOrNull()
            return if (latitude != null && longitude != null) {
                Address.Location(latitude, longitude)
            } else {
                null
            }
        }

        val location = collection.featureMembers.getOrNull(0)
            ?.geoObject?.point?.pos?.toLocation()
        return if (location != null) {
            AddressGeocode(
                collection.property.metaData.request,
                location
            )
        } else {
            null
        }
    }

    @Serializable
    class GeoObjectCollection(
        @SerialName("metaDataProperty")
        val property: MetaDataProperty,
        @SerialName("featureMember")
        val featureMembers: List<FeatureMember>,
    ) {

        @Serializable
        class MetaDataProperty(
            @SerialName("GeocoderResponseMetaData")
            val metaData: GeocoderResponseMetaData,
        ) {

            @Serializable
            class GeocoderResponseMetaData(
                @SerialName("request")
                val request: String,
                // число в строке
                @SerialName("results")
                val results: Int = 0,
                @SerialName("found")
                val found: Int = 0,
            )
        }
    }

    @Serializable
    class FeatureMember(
        @SerialName("GeoObject")
        val geoObject: GeoObject,
    ) {

        @Serializable
        class GeoObject(
            val description: String,
            val name: String,
            val boundedBy: BoundedBy? = null,
            @SerialName("Point")
            val point: Point,
        ) {

            @Serializable
            class BoundedBy(
                @SerialName("Envelope")
                val envelope: Envelope
            ) {

                @Serializable
                class Envelope(
                    val lowerCorner: String,
                    val upperCorner: String,
                )
            }

            @Serializable
            class Point(
                val pos: String,
            )
        }
    }
}