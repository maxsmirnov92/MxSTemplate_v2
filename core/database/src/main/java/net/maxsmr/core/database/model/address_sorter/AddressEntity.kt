package net.maxsmr.core.database.model.address_sorter

import androidx.room.Entity
import androidx.room.PrimaryKey
import net.maxsmr.core.database.dao.UpsertDao.Companion.NO_ID
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.Address.ErrorType
import net.maxsmr.core.domain.entities.feature.address_sorter.Address.Location
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest

@Entity(tableName = "Address")
data class AddressEntity(
    val address: String,
    // @Embedded сломано после 2.8.0
    val latitude: Float? = null,
    val longitude: Float? = null,
    val distance: Float? = null,
    val duration: Long? = null,
    val isSuggested: Boolean = false,
    val locationErrorMessage: String? = null,
    val routingErrorMessage: String? = null,
) {

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    var sortOrder: Long = NO_ID
        get() {
            if (field < 0) {
                field = id
            }
            return field
        }

    fun toDomain(): Address {
        val exceptionsMap = hashMapOf<ErrorType, String?>()
        locationErrorMessage?.let {
            exceptionsMap[ErrorType.LOCATION] = it
        }
        routingErrorMessage?.let {
            exceptionsMap[ErrorType.ROUTING] = it
        }
        return Address(
            id,
            address,
            if (latitude != null && longitude != null) {
                Location(latitude, longitude)
            } else {
                null
            },
            distance,
            duration,
            isSuggested,
            exceptionsMap
        )
    }

    companion object {

        @JvmStatic
        fun Address.toEntity(index: Int) = toEntity(index.toLong().takeIf { it >= 0 } ?: id)

        @JvmStatic
        fun Address.toEntity(sortOrder: Long) = AddressEntity(
            address = address,
            latitude = location?.latitude,
            longitude = location?.longitude,
            distance = distance,
            duration = duration,
            isSuggested = isSuggested,
            locationErrorMessage = errorMessagesMap[ErrorType.LOCATION],
            routingErrorMessage = errorMessagesMap[ErrorType.ROUTING],
        ).apply {
            this.id = this@toEntity.id.takeIf { it >= 0 } ?: 0
            this.sortOrder = sortOrder
        }

        @JvmStatic
        fun AddressSuggest.toEntity(
            id: Long,
            sortOrder: Long,
            location: Address.Location? = null,
            locationErrorMessage: String? = null,
        ): AddressEntity {
            val latitude: Float?
            val longitude: Float?
            (location ?: this.location).let {
                latitude = it?.latitude
                longitude = it?.longitude
            }
            return AddressEntity(
                address = displayedAddress,
                latitude = latitude,
                longitude = longitude,
                distance = distance,
                isSuggested = true,
                locationErrorMessage = locationErrorMessage
            ).apply {
                this.id = id
                this.sortOrder = sortOrder
            }
        }
    }
}