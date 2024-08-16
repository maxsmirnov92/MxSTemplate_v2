package net.maxsmr.core.domain.entities.feature.address_sorter

import kotlinx.serialization.Serializable

@Serializable
data class AddressSuggest(
    val location: Address.Location?,
    val address: String,
    val distance: Float?,
)