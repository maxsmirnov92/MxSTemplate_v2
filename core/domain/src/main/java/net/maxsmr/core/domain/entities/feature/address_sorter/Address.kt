package net.maxsmr.core.domain.entities.feature.address_sorter

import kotlinx.serialization.Serializable

@Serializable
data class Address(
    val id: Long = 0,
    val location: Location? = null,
    val address: String = "",
    val distance: Int? = null,
): java.io.Serializable {

    @Serializable
    data class Location(
        val latitude: Float,
        val longitude: Float,
    ): java.io.Serializable
}