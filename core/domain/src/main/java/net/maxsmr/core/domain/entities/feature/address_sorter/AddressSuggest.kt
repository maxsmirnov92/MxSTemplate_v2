package net.maxsmr.core.domain.entities.feature.address_sorter

import kotlinx.serialization.Serializable

/**
 * @param displayedAddress адрес для отображения пользователю
 * @param geocodeAddress адрес для запроса геокодирования
 */
@Serializable
data class AddressSuggest(
    val displayedAddress: String,
    val geocodeAddress: String,
    val location: Address.Location?,
    val distance: Float?,
)