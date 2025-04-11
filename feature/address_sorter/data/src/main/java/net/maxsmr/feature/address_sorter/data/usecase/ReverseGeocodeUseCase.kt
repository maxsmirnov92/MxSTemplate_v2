package net.maxsmr.feature.address_sorter.data.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import net.maxsmr.core.android.coroutines.execute.usecase.UseCase
import net.maxsmr.core.android.exceptions.EmptyResultException
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressGeocode
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import net.maxsmr.core.network.api.GeocodeDataSource
import javax.inject.Inject

/**
 * Вызывается для уточнения геопозиции у выбранного [AddressSuggest]
 */
class ReverseGeocodeUseCase @Inject constructor(
    private val geocodeDataSource: GeocodeDataSource,
    @ApplicationContext private val context: Context,
) : UseCase<Address.Location, AddressGeocode>(Dispatchers.Default) {

    override suspend fun execute(parameters: Address.Location): AddressGeocode {
        return geocodeDataSource.reverseGeocode(parameters) ?: throw EmptyResultException(context, true)
    }
}