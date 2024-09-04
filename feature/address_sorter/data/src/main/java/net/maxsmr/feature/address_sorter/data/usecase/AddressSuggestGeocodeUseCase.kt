package net.maxsmr.feature.address_sorter.data.usecase

import kotlinx.coroutines.Dispatchers
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.coroutines.usecase.UseCase
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressGeocode
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import net.maxsmr.core.network.api.GeocodeDataSource
import net.maxsmr.core.network.exceptions.EmptyResultException
import net.maxsmr.feature.address_sorter.data.getDirectDistanceByLocation
import javax.inject.Inject

/**
 * Вызывается для уточнения геопозиции у выбранного [AddressSuggest]
 */
class AddressSuggestGeocodeUseCase @Inject constructor(
    private val geocodeDataSource: GeocodeDataSource,
) : UseCase<AddressSuggestGeocodeUseCase.Parameters, AddressGeocode>(Dispatchers.IO) {

    override suspend fun execute(parameters: Parameters): AddressGeocode {
        val location = parameters.suggest.location
        return if (location == null) {
            // displayedAddress более полный, но с ним geocode не всегда корректен
            val requestAddress =
                parameters.suggest.geocodeAddress.takeIf { it.isNotEmpty() } ?: parameters.suggest.displayedAddress
            // у Яндекса в ответе suggest нет location - отдельный запрос геокодирования
            val geocode = if (requestAddress.contains(ADDRESS_DIVIDERS_REGEX)) {
                // из-за названия объекта, идущим до разделителя,
                // геокодирование выдаёт неподходящие результаты
                var result = requestAddress
                ADDRESS_DIVIDERS.forEach { divider ->
                    result.substringAfterLast(divider).takeIf { it.isNotEmpty() }?.let {
                        result = it
                    }
                }
                result.trim().takeIf { it.isNotEmpty() } ?: requestAddress.trim()
            } else {
                requestAddress.trim()
            }
            if (geocode.isEmpty()) {
                throw EmptyResultException(baseApplicationContext, false)
            }
            geocodeDataSource.directGeocode(geocode, parameters.lastLocation?.let { lastLocation ->
                {
                    getDirectDistanceByLocation(it, lastLocation)
                }
            })?.let { result ->
                parameters.suggest.displayedAddress.takeIf { it.isNotEmpty() }?.let {
                    result.copy(name = it)
                } ?: result
            } ?: throw EmptyResultException(baseApplicationContext, true)

        } else {
            // если координаты есть от другого API
            AddressGeocode(parameters.suggest.displayedAddress, location)
        }
    }

    /**
     * @param lastLocation может потребоваться для выбора
     * наиболее близкого результата из GeocodeResponse
     */
    class Parameters(
        val suggest: AddressSuggest,
        val lastLocation: Address.Location?,
    )

    companion object {

        private val ADDRESS_DIVIDERS = listOf('·')
        private val ADDRESS_DIVIDERS_REGEX = Regex("[${ADDRESS_DIVIDERS.joinToString("")}]")
    }
}