package net.maxsmr.core.database.model.address_sorter

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import net.maxsmr.core.database.dao.UpsertDao.Companion.NO_ID
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.Address.ErrorType
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest

@Entity(tableName = "Address")
data class AddressEntity(
    val address: String,
    @Embedded("location_")
    val location: Address.Location? = null,
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AddressEntity) return false

        if (address != other.address) return false
        if (location != other.location) return false
        if (distance != other.distance) return false
        if (duration != other.duration) return false
        if (isSuggested != other.isSuggested) return false
        if (locationErrorMessage != other.locationErrorMessage) return false
        if (routingErrorMessage != other.routingErrorMessage) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + (location?.hashCode() ?: 0)
        result = 31 * result + (distance?.hashCode() ?: 0)
        result = 31 * result + (duration?.hashCode() ?: 0)
        result = 31 * result + isSuggested.hashCode()
        result = 31 * result + (locationErrorMessage?.hashCode() ?: 0)
        result = 31 * result + (routingErrorMessage?.hashCode() ?: 0)
        result = 31 * result + id.hashCode()
        return result
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
            location,
            distance,
            duration,
            isSuggested,
            exceptionsMap
        )
    }

    companion object {

        @JvmStatic
        fun Address.toEntity(index: Int) = AddressEntity(
            address = address,
            location = location,
            distance = distance,
            duration = duration,
            isSuggested = isSuggested,
            locationErrorMessage = errorMessagesMap[ErrorType.LOCATION],
            routingErrorMessage = errorMessagesMap[ErrorType.ROUTING],
        ).apply {
            this.id = this@toEntity.id.takeIf { it >= 0 } ?: 0
            this.sortOrder = index.toLong().takeIf { it >= 0 } ?: id
        }

        @JvmStatic
        fun AddressSuggest.toEntity(
            id: Long,
            sortOrder: Long,
            location: Address.Location? = null,
            locationErrorMessage: String? = null,
        ) = AddressEntity(
            address = displayedAddress,
            location = location ?: this.location,
            distance = distance,
            isSuggested = true,
            locationErrorMessage = locationErrorMessage
        ).apply {
            this.id = id
            this.sortOrder = sortOrder
        }
    }
}