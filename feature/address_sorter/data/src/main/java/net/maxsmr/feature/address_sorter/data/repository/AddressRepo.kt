package net.maxsmr.feature.address_sorter.data.repository

import kotlinx.coroutines.flow.SharedFlow
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.coroutines.usecase.UseCaseResult
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressGeocode
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest

interface AddressRepo {

    val lastAddresses: SharedFlow<List<Address>>

    suspend fun add(items: List<Address>, rewrite: Boolean = false)

    suspend fun addNew(query: String = EMPTY_STRING): Address

    suspend fun getAll(): List<Address>

    suspend fun get(id: Long): Address?

    suspend fun delete(id: Long)

    suspend fun update(id: Long, updateFunc: (Address) -> Address)

    suspend fun clear()

    suspend fun reload()

    suspend fun specifyFromSuggest(
        id: Long,
        suggest: AddressSuggest,
        geocodeResult: UseCaseResult<AddressGeocode>
    )

    suspend fun updateSortOrder(ids: List<Long>)

    suspend fun upsertItemsWithSort(items: MutableList<Address>)

    /**
     * Апдейт существующей Entity в таблице при вводе или создание новой при [id] null
     */
    suspend fun updateQuery(id: Long?, query: String): Address
}