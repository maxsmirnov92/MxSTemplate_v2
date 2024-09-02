package net.maxsmr.core.network.api.doublegis

import kotlinx.serialization.Serializable
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.RoutingMode
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.RoutingType

/**
 * @param points Массив точек.
 * @param sources Какие точки из массива [points] являются точками отправления (массив индексов).
 * @param targets Какие точки из массива [points] являются точками прибытия (массив индексов).
 * @param mode Тип движения
 * @param type Тип маршрута
 */
@Serializable
class RoutingRequest(
    val points: List<Point>,
    val sources: List<Int>,
    val targets: List<Int>,
    val mode: String,
    val type: String,
) {

    constructor(
        points: List<Point>,
        sources: List<Int>,
        targets: List<Int>,
        mode: RoutingMode,
        type: RoutingType,
    ) : this(
        points,
        sources,
        targets,
        when(mode) {
            RoutingMode.DOUBLEGIS_DRIVING -> "driving"
            RoutingMode.DOUBLEGIS_TAXI -> "taxi"
            RoutingMode.DOUBLEGIS_TRUCK -> "truck"
            RoutingMode.DOUBLEGIS_WALKING -> "walking"
            RoutingMode.DOUBLEGIS_BICYCLE -> "bicycle"
            else -> throw IllegalArgumentException("Incorrect RoutingMode: $mode")
        },
        when(type) {
            RoutingType.JAM -> "jam"
            RoutingType.SHORTEST -> "shortest"
        }
    )

    @Serializable
    class Point(
        val lat: Float,
        val lon: Float
    ) {

        constructor(location: Address.Location): this(
            location.latitude,
            location.longitude
        )
    }
}