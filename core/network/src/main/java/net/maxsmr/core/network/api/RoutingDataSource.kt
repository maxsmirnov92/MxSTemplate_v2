package net.maxsmr.core.network.api

import net.maxsmr.core.domain.entities.feature.address_sorter.routing.AddressRoute
import net.maxsmr.core.network.api.doublegis.DoubleGisRoutingDataService
import net.maxsmr.core.network.api.doublegis.RoutingRequest
import net.maxsmr.core.network.api.doublegis.RoutingResponse.Route
import net.maxsmr.core.network.client.retrofit.CommonRetrofitClient

interface RoutingDataSource {

    /**
     * @return табличный id целевой точки -> посчитанный [AddressRoute] со статусом
     */
    suspend fun getDistanceMatrix(
        request: RoutingRequest,
        toAddressIdFunc: (Long) -> Long,
    ): Map<Long, Pair<AddressRoute?, Route.Status>>
}

class DoubleGisRoutingDataSource(
    private val retrofit: CommonRetrofitClient,
) : RoutingDataSource {

    override suspend fun getDistanceMatrix(
        request: RoutingRequest,
        toAddressIdFunc: (Long) -> Long,
    ): Map<Long, Pair<AddressRoute?, Route.Status>> {
        return DoubleGisRoutingDataService.instance(retrofit).getDistanceMatrix(request).asDomain(toAddressIdFunc)
    }
}