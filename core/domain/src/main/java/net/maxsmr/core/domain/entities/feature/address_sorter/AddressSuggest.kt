package net.maxsmr.core.domain.entities.feature.address_sorter

import kotlinx.serialization.Serializable

@Serializable
data class AddressSuggest(
    val address: String,
    val location: Address.Location?,
    val distance: Float?,
)