package net.maxsmr.feature.address_sorter.data.usecase

import kotlinx.coroutines.Dispatchers
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.coroutines.usecase.UseCase
import net.maxsmr.core.database.model.address_sorter.AddressEntity
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.AddressRoute
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.RoutingMode
import net.maxsmr.core.network.api.RoutingDataSource
import net.maxsmr.core.network.api.SuggestDataSource
import net.maxsmr.core.network.api.doublegis.RoutingRequest
import net.maxsmr.core.network.api.doublegis.RoutingResponse.Route
import net.maxsmr.core.network.exceptions.EmptyResultException
import net.maxsmr.feature.address_sorter.data.R
import net.maxsmr.feature.address_sorter.data.repository.AddressRepo
import net.maxsmr.feature.address_sorter.data.usecase.exceptions.MissingLocationException
import net.maxsmr.feature.address_sorter.data.usecase.exceptions.RoutingFailedException
import net.maxsmr.feature.address_sorter.data.getDirectDistanceByLocation
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import javax.inject.Inject

class AddressSortUseCase @Inject constructor(
    private val addressRepo: AddressRepo,
    private val settingsRepo: SettingsDataStoreRepository,
    private val routingDataSource: RoutingDataSource,
    private val suggestDataSource: SuggestDataSource,
) : UseCase<Address.Location?, List<AddressEntity>>(Dispatchers.IO) {

    override suspend fun execute(parameters: Address.Location?): List<AddressEntity> {
        val entities = addressRepo.getItems()

        val settings = settingsRepo.getSettings()
        val mode = settings.routingMode
        val type = settings.routingType

        val missingLocationIds = mutableListOf<Long>()
        val failRouteIds = mutableListOf<Pair<Long, Route.Status>>()

        val newEntities = if (parameters != null) {

            if (mode.isApi) {

                val routePairs: Map<Long, Pair<AddressRoute?, Route.Status>>

                if (mode == RoutingMode.SUGGEST) {

                    routePairs = mutableMapOf()
                    routePairs as MutableMap<Int, Pair<AddressRoute?, Route.Status>>
                    entities.forEach {
                        val distance =
                            suggestDataSource.suggest(it.address, parameters).getOrNull(0)?.distance
                        val routePair = if (distance != null) {
                            AddressRoute(it.id, distance, null) to Route.Status.OK
                        } else {
                            null to Route.Status.FAIL
                        }
                        routePairs[it.id] = routePair
                        if (routePair.second != Route.Status.OK) {
                            failRouteIds.add(it.id to routePair.second)
                        }
                    }
                } else {

                    val points = entities.associateBy({
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

                entities.map { entity ->
                    val routePair = routePairs[entity.id]
                    if (routePair != null) {
                        val route = routePair.first
                        if (routePair.second == Route.Status.OK && route != null) {
                            entity.copy(
                                distance = route.distance.toFloat(),
                                duration = route.duration
                            )
                        } else {
                            entity.copy(
                                routingErrorMessage = routePair.second.id
                            )
                        }.apply {
                            this.id = entity.id
                        }
                    } else {
                        // route из ответа скорее всего отсутствует по причине того, что этот entity был без location
                        if (entity.isSuggested) {
                            // предполагается быть с location
                            missingLocationIds.add(entity.id)
                            entity.copy(
                                routingErrorMessage = baseApplicationContext.getString(R.string.address_sorter_error_missing_location)
                            )
                        } else {
                            entity
                        }
                    }
                }.toMutableList()
            } else {
                if (mode == RoutingMode.DIRECT) {
                    entities.map {
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
                            // актуализация пересчитанным валидным значением
                            it.copy(
                                distance = distance,
                                duration = null
                            ).apply {
                                this.id = it.id
                            }
                        } else {
                            if (location != null) {
                                failRouteIds.add(it.id to Route.Status.FAIL)
                                it.copy(
                                    // "неизвестная ошибка"
                                    routingErrorMessage = EMPTY_STRING
                                )
                            } else {
                                if (it.isSuggested) {
                                    it.copy(
                                        routingErrorMessage = baseApplicationContext.getString(R.string.address_sorter_error_missing_location)
                                    )
                                } else {
                                    it
                                }
                            }
                        }
                    }.toMutableList()
                } else {
                    entities.toMutableList()
                }
            }
        } else {
            // отсутствие последней известной геолокации не является поводом для отказа в сортировке
            entities.toMutableList()
        }

        addressRepo.upsertItemsWithSort(newEntities)

        if (missingLocationIds.isNotEmpty()) {
            throw MissingLocationException(missingLocationIds)
        }

        if (failRouteIds.isNotEmpty()) {
            throw RoutingFailedException(failRouteIds)
        }

        return newEntities
    }
}