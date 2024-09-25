package net.maxsmr.feature.address_sorter.data.usecase

import kotlinx.coroutines.Dispatchers
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.coroutines.usecase.UseCase
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.AddressRoute
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.RoutingMode
import net.maxsmr.core.network.api.RoutingDataSource
import net.maxsmr.core.network.api.SuggestDataSource
import net.maxsmr.core.network.api.doublegis.RoutingRequest
import net.maxsmr.core.network.api.doublegis.RoutingResponse.Route
import net.maxsmr.core.android.exceptions.EmptyResultException
import net.maxsmr.feature.address_sorter.data.getDirectDistanceByLocation
import net.maxsmr.feature.address_sorter.data.repository.AddressRepo
import net.maxsmr.feature.address_sorter.data.usecase.exceptions.MissingLastLocationException
import net.maxsmr.feature.address_sorter.data.usecase.exceptions.MissingLocationException
import net.maxsmr.feature.address_sorter.data.usecase.exceptions.RoutingFailedException
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import javax.inject.Inject

class AddressRoutingUseCase @Inject constructor(
    private val addressRepo: AddressRepo,
    private val cacheRepo: CacheDataStoreRepository,
    private val settingsRepo: SettingsDataStoreRepository,
    private val routingDataSource: RoutingDataSource,
    private val suggestDataSource: SuggestDataSource,
) : UseCase<AddressRoutingUseCase.Params, AddressRoute>(Dispatchers.Default) {

    override suspend fun execute(parameters: Params): AddressRoute {
        val settings = settingsRepo.getSettings()
        val mode = settings.routingMode
        val type = settings.routingType

        val location = parameters.location ?: throw MissingLocationException(listOf(parameters.id))

        val lastLocation = parameters.lastLocation
        if (lastLocation == null && mode != RoutingMode.NO_CHANGE) {
            // при отсутствии последней известной геолокации ни по одному из способов расчёт невозможен
            throw MissingLastLocationException()
        }

        // в этом UseCase не апдейтится routingErrorMessage в итеме
        return if (lastLocation == null || mode == RoutingMode.NO_CHANGE) {
            val item = addressRepo.getItem(parameters.id) ?: throw EmptyResultException(baseApplicationContext, false)
            val distance = item.distance ?: throw RoutingFailedException(listOf(parameters.id to Route.Status.FAIL))
            AddressRoute(parameters.id, distance, item.duration)
        } else if (mode.isApi) {

            if (mode == RoutingMode.SUGGEST) {
                val item = addressRepo.getItem(parameters.id) ?: throw EmptyResultException(baseApplicationContext, false)
                val distance = suggestDataSource.suggest(item.address, lastLocation).getOrNull(0)?.distance ?: throw EmptyResultException(baseApplicationContext, true)
                AddressRoute(parameters.id, distance, null)
            } else {

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

                val routePair: Pair<AddressRoute?, Route.Status> = try {
                    routingDataSource.getDistanceMatrix(request) {
                        if (it == 1L) {
                            // точка назначения одна и она в 1-ом индексе
                            parameters.id
                        } else {
                            -1
                        }
                    }[parameters.id] ?: throw EmptyResultException(baseApplicationContext, true)
                } catch (e: Exception) {
                    logger.e(formatException(e, "getDistanceMatrix"))
                    throw e
                }

                addressRepo.updateItem(parameters.id) {
                    val route = routePair.first
                    if (routePair.second == Route.Status.OK && route != null) {
                        it.copy(
                            distance = route.distance,
                            duration = route.duration,
                            routingErrorMessage = null
                        )
                    } else {
                        it //.copy(routingException = route.second.id)
                    }.apply {
                        this.id = it.id
                        this.sortOrder = it.sortOrder
                    }
                }

                val route = routePair.first
                if (routePair.second != Route.Status.OK || route == null) {
                    throw RoutingFailedException(listOf(parameters.id to routePair.second))
                }

                route
            }
        } else {
            val distance = getDirectDistanceByLocation(location, lastLocation)

            if (distance != null) {
                addressRepo.updateItem(parameters.id) {
                    it.copy(
                        distance = distance,
                        duration = null,
                        routingErrorMessage = null
                    ).apply {
                        this.id = it.id
                        this.sortOrder = it.sortOrder
                    }
                }
            } else {
                throw RoutingFailedException(listOf(parameters.id to Route.Status.FAIL))
            }

            AddressRoute(parameters.id, distance, null)
        }
    }

    data class Params(
        val id: Long,
        val location: Address.Location?,
        val lastLocation: Address.Location?
    )
}