package net.maxsmr.core.database.model.address_sorter

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.Address.ExceptionType
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import java.lang.Exception

@Entity(tableName = "Address")
data class AddressEntity(
    val address: String,
    @Embedded("location_")
    val location: Address.Location? = null,
    val distance: Float? = null,
    val duration: Long? = null,
    val isSuggested: Boolean = false,
    val locationException: String? = null,
    val routingException: String? = null,
) {

    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    var sortOrder = NO_ID
        get() {
            if (field == NO_ID) {
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
        if (locationException != other.locationException) return false
        if (routingException != other.routingException) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + (location?.hashCode() ?: 0)
        result = 31 * result + (distance?.hashCode() ?: 0)
        result = 31 * result + (duration?.hashCode() ?: 0)
        result = 31 * result + isSuggested.hashCode()
        result = 31 * result + (locationException?.hashCode() ?: 0)
        result = 31 * result + (routingException?.hashCode() ?: 0)
        result = 31 * result + id.hashCode()
        return result
    }

    fun toDomain(): Address {
        val exceptionsMap = hashMapOf<ExceptionType, String?>()
        locationException?.let {
            exceptionsMap[ExceptionType.LOCATION] = it
        }
        routingException?.let {
            exceptionsMap[ExceptionType.ROUTING] = it
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

        const val NO_ID = -1L

        @JvmStatic
        fun Address.toEntity() = AddressEntity(
            address = address,
            location = location,
            distance = distance,
            duration = duration
        )

        @JvmStatic
        fun AddressSuggest.toEntity(
            id: Long,
            sortOrder: Long,
            location: Address.Location? = null,
            exception: Exception? = null,
        ) = AddressEntity(
            address = address,
            location = location ?: this.location,
            distance = distance,
            isSuggested = true,
            locationException = exception?.message
        ).apply {
            this.id = id
            this.sortOrder = sortOrder
        }
    }
}