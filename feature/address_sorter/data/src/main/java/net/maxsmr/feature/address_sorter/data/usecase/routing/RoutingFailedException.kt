package net.maxsmr.feature.address_sorter.data.usecase.routing

import net.maxsmr.core.domain.entities.feature.address_sorter.routing.AddressRoute
import net.maxsmr.core.network.api.doublegis.RoutingResponse.Route

/**
 * @param routes key - id точки назначения, value - неудачный статус
 */
class RoutingFailedException(
    val routes: List<Pair<Long, Route.Status>>
): RuntimeException() {

    init {
        if (routes.isEmpty()) {
            throw IllegalArgumentException("routes is empty")
        }
    }
}