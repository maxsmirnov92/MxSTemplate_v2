package net.maxsmr.core.network.api.yandex.geocode

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.core.domain.StringId
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressGeocode
import net.maxsmr.core.network.retrofit.serializers.StringIdEnumSerializer

/**
 * Ответ геокодера
 * @param collection Корневая коллекция геообъектов.
 */
@Serializable
class GeocodeResponse(
    @SerialName("GeoObjectCollection")
    val collection: GeoObjectCollection,
) {

    fun asDirectGeocodeDomain(getDistanceForLocationFunc: ((Address.Location) -> Float?)?): AddressGeocode? {

        val firstObject = collection.featureMembers.getOrNull(0)?.geoObject?.let {
            Pair(it, it.point?.asLocation())
        }

        val obj = if (getDistanceForLocationFunc != null && collection.featureMembers.size > 1) {
            // самый близкий из нескольких к последней известной геопозиции считаем наиболее релевантным
            collection.featureMembers
                .mapNotNull {
                    val geoObject = it.geoObject
                    val location = geoObject.point?.asLocation()
                    if (location != null) {
                        val distance = getDistanceForLocationFunc(location)
                        if (distance != null) {
                            Triple(geoObject, location, distance)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
                .minByOrNull { it.third }?.let { Pair(it.first, it.second) } ?: firstObject
        } else {
            firstObject
        }

        val location = obj?.second
        return if (location != null) {
            AddressGeocode(
                collection.property.metaData.suggest?.takeIf { it.isNotEmpty() } ?:
                collection.property.metaData.request,
                location,
                obj.first.description,
            )
        } else {
            null
        }
    }

    fun asReverseGeocodeDomain(location: Address.Location?): AddressGeocode? {
        return collection.featureMembers.getOrNull(0)?.geoObject?.let { obj ->
            val targetLocation = location ?: obj.point?.asLocation() ?: return null
            obj.name.takeIf { it.isNotEmpty() }?.let {
                AddressGeocode(it, targetLocation, obj.description)
            }
        }
    }

    /**
     * @param property Метаданные коллекции геообъектов.
     * @param featureMembers Список геообъектов
     */
    @Serializable
    class GeoObjectCollection(
        @SerialName("metaDataProperty")
        val property: MetaDataProperty,
        @SerialName("featureMember")
        val featureMembers: List<FeatureMember> = listOf(),
    ) {

        @Serializable
        class MetaDataProperty(
            @SerialName("GeocoderResponseMetaData")
            val metaData: GeocoderResponseMetaData,
        ) {

            @Serializable
            class GeocoderResponseMetaData(
                val request: String,
                val suggest: String? = null,
                // число в строке
                val results: Int = 0,
                val found: Int = 0,
            )
        }
    }

    /**
     * @param geoObject Геообъект коллекции
     */
    @Serializable
    class FeatureMember(
        @SerialName("GeoObject")
        val geoObject: GeoObject,
    ) {

        /**
         * @param name Текст, который рекомендуется указывать в качестве заголовка при отображении найденного объекта.
         * @param description Текст, который рекомендуется указывать в качестве подзаголовка при отображении найденного объекта.
         * @param boundedBy Границы области, в которую входит организация. Содержит координаты левого нижнего и правого верхнего углов области.
         * Координаты указаны в последовательности «долгота, широта».
         * @param uri ID найденного объекта
         * @param point координаты объекта
         */
        @Serializable
        class GeoObject(
            val metaDataProperty: MetaDataProperty,
            val name: String,
            val description: String? = null,
            val boundedBy: BoundedBy? = null,
            val uri: String? = null,
            @SerialName("Point")
            val point: Point? = null,
        ) {

            @Serializable
            class MetaDataProperty(
                @SerialName("GeocoderMetaData")
                val metaData: GeocoderMetaData
            ) {

                @Serializable
                class GeocoderMetaData(
                    val kind: String? = null,
                    @Serializable(Precision.Serializer::class)
                    val precision: Precision? = null
                ) {

                    enum class Precision(override val id: String): StringId {

                        EXACT("exact"),
                        NUMBER("number"),
                        NEAR("near"),
                        RANGE("range"),
                        STREET("street"),
                        OTHER("other");

                        object Serializer : StringIdEnumSerializer<Precision>(
                            Precision::class,
                            Precision.entries.toTypedArray(),
                            EXACT
                        )
                    }
                }
            }

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
            ) {

                fun asLocation(): Address.Location? {
                    val pos = pos.split(" ").orEmpty()
                    val latitude = pos.getOrNull(1)?.toFloatOrNull()
                    val longitude = pos.getOrNull(0)?.toFloatOrNull()
                    return if (latitude != null && longitude != null) {
                        Address.Location(latitude, longitude)
                    } else {
                        null
                    }
                }

            }
        }
    }
}