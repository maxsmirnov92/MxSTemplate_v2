package net.maxsmr.feature.address_sorter.data.usecase.exceptions

import net.maxsmr.core.network.api.doublegis.RoutingResponse.Route

/**
 * @param routes key - id точки назначения, value - неудачный статус
 */
class RoutingFailedException(
    val routes: List<Pair<Long, Route.Status>>
): RuntimeException() {

    init {
        if (routes.none { it.second != Route.Status.OK }) {
            throw IllegalArgumentException("Failed routes is empty")
        }
    }
}