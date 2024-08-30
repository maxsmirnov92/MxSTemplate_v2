package net.maxsmr.feature.address_sorter.data.usecase

import kotlinx.coroutines.Dispatchers
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.coroutines.usecase.UseCase
import net.maxsmr.core.database.model.address_sorter.AddressEntity
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.AddressRoute
import net.maxsmr.core.network.api.RoutingDataSource
import net.maxsmr.core.network.api.doublegis.RoutingRequest
import net.maxsmr.core.network.api.doublegis.RoutingResponse.Route
import net.maxsmr.core.network.exceptions.EmptyResultException
import net.maxsmr.feature.address_sorter.data.R
import net.maxsmr.feature.address_sorter.data.repository.AddressRepo
import net.maxsmr.feature.address_sorter.data.usecase.routing.MissingLocationException
import net.maxsmr.feature.address_sorter.data.usecase.routing.RoutingFailedException
import net.maxsmr.feature.address_sorter.data.usecase.routing.getDirectDistanceByLocation
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import javax.inject.Inject

class AddressSortUseCase @Inject constructor(
    private val addressRepo: AddressRepo,
    private val cacheRepo: CacheDataStoreRepository,
    private val settingsRepo: SettingsDataStoreRepository,
    private val routingDataSource: RoutingDataSource,
) : UseCase<Unit, List<AddressEntity>>(Dispatchers.IO) {

    override suspend fun execute(parameters: Unit): List<AddressEntity> {
        val entities = addressRepo.getItems()
        val lastLocation = cacheRepo.getLastLocation()

        val settings = settingsRepo.getSettings()
        val mode = settings.routingMode
        val type = settings.routingType

        val missingLocationIds = mutableListOf<Long>()
        val failRouteIds = mutableListOf<Pair<Long, Route.Status>>()

        val newEntities = if (lastLocation != null) {
            if (mode.isApi) {
                val points = entities.associateBy({
                    it.id
                }) {
                    val location = it.location ?: return@associateBy null
                    RoutingRequest.Point(location)
                }.toMutableMap().apply {
                    // по нулевому id дописываем точку, от которой считать до всех остальных
                    this[0] = RoutingRequest.Point(lastLocation)
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

                val routes: List<Pair<AddressRoute, Route.Status>> = try {
                    routingDataSource.getDistanceMatrix(request) {
                        points.getOrNull(it.toInt())?.key ?: -1
                    }.takeIf { it.isNotEmpty() } ?: throw EmptyResultException(baseApplicationContext)
                } catch (e: Exception) {
                    logger.e(formatException(e, "getDistanceMatrix"))
                    throw e
                }

                val okRoutes = mutableListOf<AddressRoute>()
                val failRoutes = mutableListOf<Pair<AddressRoute, Route.Status>>()
                routes.forEach {
                    if (it.second == Route.Status.OK) {
                        okRoutes.add(it.first)
                    } else {
                        failRoutes.add(it)
                    }
                }

                if (okRoutes.isNotEmpty() || failRoutes.isNotEmpty()) {

                    if (failRoutes.isNotEmpty()) {
                        failRouteIds.addAll(failRoutes.map { it.first.id to it.second })
                    }

                    entities.map { entity ->
                        val route = routes.find { it.first.id == entity.id }
                        if (route != null) {
                            if (route.second == Route.Status.OK) {
                                entity.copy(
                                    distance = route.first.distance.toFloat(),
                                    duration = route.first.duration
                                )
                            } else {
                                entity.copy(
                                    routingErrorMessage = route.second.id
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
                    entities.toMutableList()
                }
            } else {
                entities.map {
                    val location = it.location
                    val distance = if (location != null) {
                        getDirectDistanceByLocation(location, lastLocation)
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