package net.maxsmr.feature.address_sorter.data

import androidx.annotation.StringRes
import net.maxsmr.core.network.api.doublegis.RoutingResponse

@StringRes
fun RoutingResponse.Route.Status.getDisplayedMessageResId(): Int =
    when (this) {
        RoutingResponse.Route.Status.OK -> R.string.address_sorter_route_build_status_ok
        RoutingResponse.Route.Status.FAIL -> R.string.address_sorter_route_build_status_fail
        RoutingResponse.Route.Status.POINT_EXCLUDED -> R.string.address_sorter_route_build_status_point_excluded
        RoutingResponse.Route.Status.ROUTE_NOT_FOUND -> R.string.address_sorter_route_build_status_route_not_found
        RoutingResponse.Route.Status.ROUTE_DOES_NOT_EXISTS -> R.string.address_sorter_route_build_status_route_does_not_exists
        RoutingResponse.Route.Status.ATTRACT_FAIL -> R.string.address_sorter_route_build_status_route_attract_fail
    }