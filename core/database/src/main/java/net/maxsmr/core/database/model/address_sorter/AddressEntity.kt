package net.maxsmr.core.database.model.address_sorter

import androidx.room.Entity
import androidx.room.PrimaryKey
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest

@Entity(tableName = "Address")
data class AddressEntity(
    val address: String,
    val latitude: Float? = null,
    val longitude: Float? = null,
    val distance: Float? = null,
    val isSuggested: Boolean = false,
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



    fun toDomain() = Address(
        id,
        if (latitude != null && longitude != null) {
            Address.Location(latitude, longitude)
        } else {
            null
        },
        address,
        distance,
        isSuggested
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AddressEntity) return false

        if (address != other.address) return false
        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (distance != other.distance) return false
        if (isSuggested != other.isSuggested) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + (latitude?.hashCode() ?: 0)
        result = 31 * result + (longitude?.hashCode() ?: 0)
        result = 31 * result + (distance?.hashCode() ?: 0)
        result = 31 * result + isSuggested.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }

    companion object {

        const val NO_ID = -1L

        @JvmStatic
        fun Address.toAddressEntity() = AddressEntity(
            address = address,
            latitude = location?.latitude,
            longitude = location?.longitude,
            distance = distance
        )

        @JvmStatic
        fun AddressSuggest.toAddressEntity(id: Long, sortOrder: Long) = AddressEntity(
            address = address,
            latitude = location?.latitude,
            longitude = location?.longitude,
            distance = distance,
            isSuggested = true
        ).apply {
            this.id = id
            this.sortOrder = sortOrder
        }
    }
}