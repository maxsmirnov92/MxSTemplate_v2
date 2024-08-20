package net.maxsmr.feature.address_sorter.data.repository

import android.location.Location
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import java.io.InputStream

interface AddressRepo {

    val resultAddresses: StateFlow<List<Address>>

    val upsertCompletedEvent: SharedFlow<Unit>

    suspend fun addFromStream(stream: InputStream, rewrite: Boolean = false): Boolean

    suspend fun addNewItem(query: String = EMPTY_STRING)

    suspend fun deleteItem(id: Long)

    suspend fun clearItems()

    suspend fun specifyItem(id: Long, suggest: AddressSuggest)

    suspend fun updateSortOrder(ids: List<Long>)

    suspend fun sortItems()

    suspend fun suggest(query: String): List<AddressSuggest>

    suspend fun suggestWithUpdate(id: Long, query: String): List<AddressSuggest>

    suspend fun updateQuery(id: Long?, query: String)

    suspend fun setLastLocation(location: Location?)
}