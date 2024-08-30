package net.maxsmr.core.domain.entities.feature.address_sorter

import kotlinx.serialization.Serializable

@Serializable
data class Address(
    val id: Long,
    val address: String,
    val location: Location? = null,
    val distance: Float? = null,
    val duration: Long? = null,
    val isSuggested: Boolean = false,
    val errorMessagesMap: HashMap<ErrorType, String?> = hashMapOf(),
) : java.io.Serializable {

    @Serializable
    data class Location(
        val latitude: Float,
        val longitude: Float,
    ) : java.io.Serializable {

        fun asReadableString(isLatFirst: Boolean) = if (isLatFirst) {
            "$latitude, $longitude"
        } else {
            "$longitude, $latitude"
        }

        override fun toString(): String {
            return "Location(latitude=$latitude, longitude=$longitude)"
        }
    }

    enum class ErrorType {

        LOCATION,
        ROUTING
    }
}