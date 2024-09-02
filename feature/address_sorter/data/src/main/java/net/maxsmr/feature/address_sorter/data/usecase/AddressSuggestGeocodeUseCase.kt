package net.maxsmr.feature.address_sorter.data.usecase

import kotlinx.coroutines.Dispatchers
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.coroutines.usecase.UseCase
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressGeocode
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import net.maxsmr.core.network.api.GeocodeDataSource
import net.maxsmr.core.network.exceptions.EmptyResultException
import net.maxsmr.feature.address_sorter.data.usecase.routing.getDirectDistanceByLocation
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import javax.inject.Inject

/**
 * Вызывается для уточнения геопозиции у выбранного [AddressSuggest]
 */
class AddressSuggestGeocodeUseCase @Inject constructor(
    private val cacheRepo: CacheDataStoreRepository,
    private val geocodeDataSource: GeocodeDataSource,
) : UseCase<AddressSuggest, AddressGeocode>(Dispatchers.IO) {

    override suspend fun execute(parameters: AddressSuggest): AddressGeocode {
        val lastLocation = cacheRepo.getLastLocation()
        val location = parameters.location
        return if (location == null) {
            // у Яндекса в ответе suggest нет location - отдельный запрос геокодирования
            val geocode = if (parameters.address.contains(ADDRESS_DIVIDERS_REGEX)) {
                // из-за названия объекта, идущим до разделителя,
                // геокодирование выдаёт неподходящие результаты
                var result = parameters.address
                ADDRESS_DIVIDERS.forEach { divider ->
                    result.substringAfterLast(divider).takeIf { it.isNotEmpty() }?.let {
                        result = it
                    }
                }
                result
            } else {
                parameters.address
            }
            geocodeDataSource.directGeocode(geocode, if (lastLocation != null) {
                {
                    getDirectDistanceByLocation(it, lastLocation)
                }
            } else {
                null
            }) ?: throw EmptyResultException(baseApplicationContext, true)

        } else {
            // если координаты есть от другого API
            AddressGeocode(parameters.address, location)
        }
    }

    companion object {

        private val ADDRESS_DIVIDERS = listOf('·')
        private val ADDRESS_DIVIDERS_REGEX = Regex("[${ADDRESS_DIVIDERS.joinToString("")}]")
    }
}