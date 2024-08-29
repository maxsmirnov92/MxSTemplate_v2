package net.maxsmr.feature.address_sorter.data.repository

import android.location.Location
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.coroutines.usecase.UseCaseResult
import net.maxsmr.core.database.model.address_sorter.AddressEntity
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressGeocode
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import java.io.InputStream

interface AddressRepo {

    val resultAddresses: StateFlow<List<Address>>

    val upsertCompletedEvent: SharedFlow<Unit>

    suspend fun addFromStream(stream: InputStream, rewrite: Boolean = false): Boolean

    suspend fun addNewItem(query: String = EMPTY_STRING)

    suspend fun getItems(): List<AddressEntity>

    suspend fun deleteItem(id: Long)

    suspend fun updateItem(id: Long, updateFunc: (AddressEntity) -> AddressEntity)

    suspend fun clearItems()

    suspend fun specifyFromSuggest(
        id: Long,
        suggest: AddressSuggest,
        geocodeResult: UseCaseResult<AddressGeocode>
    )

    suspend fun updateSortOrder(ids: List<Long>)

    suspend fun upsertItemsWithSort(items: MutableList<AddressEntity>)

    /**
     * Апдейт существующей Entity в таблице при вводе или создание новой при [id] null
     */
    suspend fun updateQuery(id: Long?, query: String)

    suspend fun setLastLocation(location: Location?)
}