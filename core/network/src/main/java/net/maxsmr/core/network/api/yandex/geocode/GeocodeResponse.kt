package net.maxsmr.core.network.api.yandex.geocode

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressGeocode

@Serializable
class GeocodeResponse(
    @SerialName("GeoObjectCollection")
    val collection: GeoObjectCollection,
) {

    fun asDomain(getDistanceForLocationFunc: ((Address.Location) -> Float?)?): AddressGeocode? {

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

        val firstLocation = collection.featureMembers.getOrNull(0)?.geoObject?.point?.pos?.toLocation()

        val location = if (getDistanceForLocationFunc != null && collection.featureMembers.size > 1) {
            // самый близкий из нескольких к последней известной геопозиции считаем наиболее релевантным
            collection.featureMembers
                .mapNotNull {
                    val location = it.geoObject.point.pos.toLocation()
                    if (location != null) {
                        val distance = getDistanceForLocationFunc(location)
                        if (distance != null) {
                            Pair(location, distance)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
                .minByOrNull { it.second }?.first ?: firstLocation
        } else {
            firstLocation
        }

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
                val envelope: Envelope,
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