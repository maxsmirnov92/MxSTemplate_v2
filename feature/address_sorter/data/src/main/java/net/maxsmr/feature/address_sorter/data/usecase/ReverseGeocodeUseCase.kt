package net.maxsmr.feature.address_sorter.data.usecase

import kotlinx.coroutines.Dispatchers
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.coroutines.usecase.UseCase
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressGeocode
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import net.maxsmr.core.network.api.GeocodeDataSource
import net.maxsmr.core.network.exceptions.EmptyResultException
import javax.inject.Inject

/**
 * Вызывается для уточнения геопозиции у выбранного [AddressSuggest]
 */
class ReverseGeocodeUseCase @Inject constructor(
    private val geocodeDataSource: GeocodeDataSource
) : UseCase<Address.Location, AddressGeocode>(Dispatchers.IO) {

    override suspend fun execute(parameters: Address.Location): AddressGeocode {
        return geocodeDataSource.reverseGeocode(parameters) ?: throw EmptyResultException(baseApplicationContext)
    }
}