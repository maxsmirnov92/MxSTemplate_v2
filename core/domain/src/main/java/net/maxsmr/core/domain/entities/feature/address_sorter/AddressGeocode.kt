package net.maxsmr.core.domain.entities.feature.address_sorter

import kotlinx.serialization.Serializable

@Serializable
data class AddressGeocode(
    val name: String,
    val location: Address.Location,
    val description: String? = null,
)