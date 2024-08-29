package net.maxsmr.feature.address_sorter.data.usecase.routing

import kotlinx.coroutines.Dispatchers
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException
import net.maxsmr.core.android.coroutines.usecase.UseCase
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.AddressRoute
import net.maxsmr.core.network.api.RoutingDataSource
import net.maxsmr.core.network.api.doublegis.RoutingRequest
import net.maxsmr.core.network.api.doublegis.RoutingResponse.Route
import net.maxsmr.core.network.exceptions.EmptyResponseException
import net.maxsmr.feature.address_sorter.data.repository.AddressRepo
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import javax.inject.Inject

class AddressRoutingUseCase @Inject constructor(
    private val addressRepo: AddressRepo,
    private val cacheRepo: CacheDataStoreRepository,
    private val settingsRepo: SettingsDataStoreRepository,
    private val routingDataSource: RoutingDataSource,
) : UseCase<AddressRoutingUseCase.Params, AddressRoute>(Dispatchers.IO) {

    override suspend fun execute(parameters: Params): AddressRoute {
        val lastLocation = cacheRepo.getLastLocation()

        val settings = settingsRepo.getSettings()
        val mode = settings.routingMode
        val type = settings.routingType

        val location = parameters.location ?: throw MissingLocationException(listOf(parameters.id))

        if (lastLocation == null) {
            // при отсутствии последней известной геолокации ни по одному из способов расчёт невозможен
            throw MissingLastLocationException()
        }

        // в этом UseCase не апдейтится routingException в итеме
        return if (mode.isApi) {
            val points = mutableListOf<RoutingRequest.Point>()
            points.add(RoutingRequest.Point(lastLocation))
            points.add(RoutingRequest.Point(location))

            val request = RoutingRequest(
                points,
                listOf(0),
                listOf(1),
                mode,
                type
            )

            val route: Pair<AddressRoute, Route.Status> = try {
                routingDataSource.getDistanceMatrix(request) {
                    if (it == 1L) {
                        // точка назначения одна и она в 1-ом индексе
                        parameters.id
                    } else {
                        -1
                    }
                }.getOrNull(0) ?: throw EmptyResponseException()
            } catch (e: Exception) {
                logger.e(formatException(e, "getDistanceMatrix"))
                throw e
            }

            addressRepo.updateItem(parameters.id) {
                if (route.second == Route.Status.OK) {
                    it.copy(
                        distance = route.first.distance.toFloat(),
                        duration = route.first.duration
                    )
                } else {
                    it //.copy(routingException = route.second.id)
                }.apply {
                    this.id = it.id
                    this.sortOrder = it.sortOrder
                }
            }

            if (route.second != Route.Status.OK) {
                throw RoutingFailedException(listOf(parameters.id to route.second))
            }

            route.first
        } else {
            val distance = getDirectDistanceByLocation(location, lastLocation)

            if (distance != null) {
                addressRepo.updateItem(parameters.id) {
                    it.copy(
                        distance = distance,
                        duration = null
                    ).apply {
                        this.id = it.id
                        this.sortOrder = it.sortOrder
                    }
                }
            } else {
                throw RoutingFailedException(listOf(parameters.id to Route.Status.FAIL))
            }

            AddressRoute(parameters.id, distance.toLong(), null)
        }
    }

    data class Params(
        val id: Long,
        val location: Address.Location?,
    )
}