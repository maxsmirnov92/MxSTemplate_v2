package net.maxsmr.feature.address_sorter.data.usecase

import kotlinx.coroutines.Dispatchers
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.coroutines.usecase.UseCase
import net.maxsmr.core.android.exceptions.EmptyResultException
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.AddressRoute
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.RoutingMode
import net.maxsmr.core.network.api.RoutingDataSource
import net.maxsmr.core.network.api.SuggestDataSource
import net.maxsmr.core.network.api.doublegis.RoutingRequest
import net.maxsmr.core.network.api.doublegis.RoutingResponse.Route
import net.maxsmr.feature.address_sorter.data.R
import net.maxsmr.feature.address_sorter.data.getDirectDistanceByLocation
import net.maxsmr.feature.address_sorter.data.getDisplayedMessageResId
import net.maxsmr.feature.address_sorter.data.repository.AddressRepo
import net.maxsmr.feature.address_sorter.data.usecase.exceptions.MissingLocationException
import net.maxsmr.feature.address_sorter.data.usecase.exceptions.RoutingFailedException
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import javax.inject.Inject

class AddressSortUseCase @Inject constructor(
    private val addressRepo: AddressRepo,
    private val settingsRepo: SettingsDataStoreRepository,
    private val routingDataSource: RoutingDataSource,
    private val suggestDataSource: SuggestDataSource,
) : UseCase<Address.Location?, List<Address>>(Dispatchers.Default) {

    override suspend fun execute(parameters: Address.Location?): List<Address> {
        val addresses = addressRepo.getAll()

        val settings = settingsRepo.getSettings()
        val mode = settings.routingMode
        val type = settings.routingType

        val missingLocationIds = mutableListOf<Long>()
        val failRouteIds = mutableListOf<Pair<Long, Route.Status>>()

        val newAddresses = if (parameters != null) {

            if (mode.isApi) {

                val routePairs: Map<Long, Pair<AddressRoute?, Route.Status>>

                if (mode == RoutingMode.SUGGEST) {
                    routePairs = mutableMapOf()
                    routePairs as MutableMap<Int, Pair<AddressRoute?, Route.Status>>
                    addresses.forEach {
                        val distance =
                            suggestDataSource.suggest(it.address, parameters).getOrNull(0)?.distance
                        val routePair = if (distance != null) {
                            AddressRoute(it.id, distance, null) to Route.Status.OK
                        } else {
                            null to Route.Status.FAIL
                        }
                        routePairs[it.id] = routePair
                    }
                } else {

                    val points = addresses.associateBy({
                        it.id
                    }) {
                        val location = it.location ?: return@associateBy null
                        RoutingRequest.Point(location)
                    }.toMutableMap().apply {
                        // по нулевому id дописываем точку, от которой считать до всех остальных
                        this[0] = RoutingRequest.Point(parameters)
                    }.mapNotNull {
                        if (it.value == null) {
                            null
                        } else {
                            it
                        }
                    } as List<Map.Entry<Long, RoutingRequest.Point>>

                    val sourceIndex = points.indexOfFirst {
                        it.key == 0L
                    }

                    val request = RoutingRequest(
                        points.map { it.value },
                        listOf(sourceIndex),
                        points.mapIndexedNotNull { index, entry ->
                            if (entry.key != 0L) {
                                index
                            } else {
                                null
                            }
                        },
                        mode,
                        type
                    )

                    routePairs = if (points.isNotEmpty()) {
                        try {
                            routingDataSource.getDistanceMatrix(request) {
                                points.getOrNull(it.toInt())?.key ?: -1
                            }.takeIf { it.isNotEmpty() }
                                ?: throw EmptyResultException(baseApplicationContext, true)
                        } catch (e: Exception) {
                            logger.e(formatException(e, "getDistanceMatrix"))
                            throw e
                        }
                    } else {
                        mapOf()
                    }
                }

                failRouteIds.addAll(routePairs.filter {
                    it.value.second != Route.Status.OK
                }.map { it.key to it.value.second })

                addresses.map { address ->
                    val newMap = address.errorMessagesMap
                    val routePair = routePairs[address.id]
                    if (routePair != null) {
                        val route = routePair.first
                        if (routePair.second == Route.Status.OK && route != null) {
                            newMap.remove(Address.ErrorType.ROUTING)
                            address.copy(
                                distance = route.distance,
                                duration = route.duration,
                                errorMessagesMap = newMap
                            )
                        } else {
                            newMap[Address.ErrorType.ROUTING] = baseApplicationContext.getString(routePair.second.getDisplayedMessageResId())
                            address.copy(
                                errorMessagesMap = newMap
                            )
                        }
                    } else {
                        // route из ответа скорее всего отсутствует по причине того, что этот address был без location
                        if (address.isSuggested) {
                            // предполагается быть с location
                            missingLocationIds.add(address.id)
                            newMap[Address.ErrorType.ROUTING] = baseApplicationContext.getString(R.string.address_sorter_error_missing_location)
                            address.copy(errorMessagesMap =  newMap)
                        } else {
                            address
                        }
                    }
                }.toMutableList()
            } else {
                if (mode == RoutingMode.DIRECT) {
                    addresses.map {
                        val newMap = it.errorMessagesMap
                        val location = it.location
                        val distance = if (location != null) {
                            getDirectDistanceByLocation(location, parameters)
                        } else {
                            if (it.isSuggested) {
                                missingLocationIds.add(it.id)
                                it.distance
                            } else {
                                null
                            }
                        }
                        if (distance != null) {
                            newMap.remove(Address.ErrorType.ROUTING)
                            // актуализация пересчитанным валидным значением
                            it.copy(
                                distance = distance,
                                duration = null,
                                errorMessagesMap = newMap
                            )
                        } else {
                            if (location != null) {
                                failRouteIds.add(it.id to Route.Status.FAIL)
                                newMap[Address.ErrorType.ROUTING] = baseApplicationContext.getString(Route.Status.FAIL.getDisplayedMessageResId())
                                it.copy(
                                    errorMessagesMap = newMap
                                )
                            } else {
                                if (it.isSuggested) {
                                    newMap[Address.ErrorType.ROUTING] =baseApplicationContext.getString(R.string.address_sorter_error_missing_location)
                                        it.copy(errorMessagesMap = newMap)
                                } else {
                                    it
                                }
                            }
                        }
                    }.toMutableList()
                } else {
                    addresses.toMutableList()
                }
            }
        } else {
            // отсутствие последней известной геолокации не является поводом для отказа в сортировке
            addresses.toMutableList()
        }

        addressRepo.upsertItemsWithSort(newAddresses)

        if (missingLocationIds.isNotEmpty()) {
            throw MissingLocationException(missingLocationIds)
        }

        if (failRouteIds.isNotEmpty()) {
            throw RoutingFailedException(failRouteIds)
        }

        return newAddresses
    }
}