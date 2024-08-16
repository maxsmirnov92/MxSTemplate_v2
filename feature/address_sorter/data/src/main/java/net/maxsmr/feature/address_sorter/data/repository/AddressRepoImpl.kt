package net.maxsmr.feature.address_sorter.data.repository

import android.graphics.PointF
import android.location.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.maxsmr.commonutils.location.distance
import net.maxsmr.commonutils.readString
import net.maxsmr.core.android.coroutines.mutableStateIn
import net.maxsmr.core.database.dao.address_sorter.AddressDao
import net.maxsmr.core.database.model.address_sorter.AddressEntity
import net.maxsmr.core.database.model.address_sorter.AddressEntity.Companion.NO_ID
import net.maxsmr.core.database.model.address_sorter.AddressEntity.Companion.toAddressEntity
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import net.maxsmr.core.network.api.radar_io.AddressDataSource
import net.maxsmr.core.utils.decodeFromStringOrNull
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import java.io.InputStream

class AddressRepoImpl(
    private val dao: AddressDao,
    private val cacheRepo: CacheDataStoreRepository,
    private val json: Json,
    private val dataSource: AddressDataSource,
) : AddressRepo {

    private val ioDispatcher = Dispatchers.IO

    private val scope = CoroutineScope(ioDispatcher + Job())

    override val resultAddresses: MutableStateFlow<List<Address>> = dao.get().map { list ->
        list.map { it.toDomain() }
    }.mutableStateIn(scope, listOf(), ioDispatcher)

    override val sortCompletedEvent: MutableSharedFlow<Unit> = MutableSharedFlow()

    override suspend fun addFromStream(stream: InputStream, rewrite: Boolean): Boolean {
        return withContext(ioDispatcher) {
            stream.readString()?.let { value ->
                json.decodeFromStringOrNull<List<Address>>(value)?.let { list ->
                    val entities = list.map { it.toAddressEntity() }
                    if (entities.isNotEmpty()) {
                        if (rewrite) {
                            dao.clear()
                        }
                        dao.upsert(entities)
                        return@withContext true
                    }
                }
            }
            return@withContext false
        }
    }

    override suspend fun addNewItem(query: String) {
        withContext(ioDispatcher) {
            updateQuery(null, query)
        }
    }

    override suspend fun deleteItem(id: Long) {
        withContext(ioDispatcher) {
            dao.deleteById(id)
        }
    }

    override suspend fun clearItems() {
        withContext(ioDispatcher) {
            dao.clear()
        }
    }

    override suspend fun specifyItem(id: Long, suggest: AddressSuggest) {
        withContext(ioDispatcher) {
            addFromSuggest(id, suggest)
        }
    }

    override suspend fun updateSortOrder(ids: List<Long>) {
        val result = mutableListOf<AddressEntity>()
        ids.forEachIndexed { index, id ->
            dao.getById(id)?.let {
                it.sortOrder = index.toLong()
                result.add(it)
            }
        }
        // отсортированные AddressEntity пишем в таблицу и ждём изменения в resultAddresses
        val resultIds = dao.upsert(result)
        if (resultIds.isEmpty() || resultIds.all { it == NO_ID }) {
            // ивент нужен для случая, когда изменений в таблице нет - StateFlow не принимает без изменений
            sortCompletedEvent.emit(Unit)
        }
    }

    override suspend fun sortItems() = withContext(ioDispatcher) {
        updateSortOrder(resultAddresses.value.sortByLocation(cacheRepo.getLastLocation()).map { it.id })
    }

    override suspend fun suggest(query: String): List<AddressSuggest> {
        return withContext(ioDispatcher) {
            val lastLocation = cacheRepo.getLastLocation()
            dataSource.suggest(query, lastLocation?.latitude, lastLocation?.longitude)
        }
    }

    override suspend fun suggestWithUpdate(id: Long, query: String): List<AddressSuggest> {
        return withContext(ioDispatcher) {
            // апдейт существующей Entity в таблице при вводе
            updateQuery(id, query)
            suggest(query)
        }
    }

    override suspend fun updateQuery(id: Long?, query: String) = withContext(ioDispatcher) {
        val maxSortOrder = dao.getRaw().maxOfOrNull { it.sortOrder } ?: NO_ID
        val current = if (id != null && id > 0) dao.getById(id) else null
        dao.upsert(current?.copy(address = query)?.apply {
            this.id = current.id
            this.sortOrder = current.sortOrder
        } ?: AddressEntity(query).apply {
            sortOrder = maxSortOrder + 1
        })
    }

    override suspend fun setLastLocation(location: Location?) = withContext(ioDispatcher) {
        cacheRepo.setLastLocation(location?.let {
            Address.Location(
                location.latitude.toFloat(),
                location.longitude.toFloat()
            )
        })
    }

    private suspend fun addFromSuggest(id: Long, addressSuggest: AddressSuggest) =
        withContext(ioDispatcher) {
            dao.upsert(addressSuggest.toAddressEntity(id))
        }

    private fun List<Address>.addIfNew(item: Address): List<Address> {
        val result = mutableListOf<Address>()
        result.addAll(this)
        if (!result.any { it.id == item.id }) {
            result.add(item)
        }
        return result
    }

    private fun List<Address>.sortByLocation(lastLocation: Address.Location?): List<Address> {

        fun Address.Location.toPointF() = PointF(latitude, longitude)

        val result = mutableListOf<Address>()
        result.addAll(this)

        result.sortBy { address ->
            var distance = address.distance ?: 0f
            if (distance <= 0) {
                distance = address.location?.toPointF()?.let {
                    lastLocation?.toPointF()?.let { lastLocation ->
                        distance(lastLocation, it).toFloat()
                    }
                } ?: 0f
            }
            distance
        }
        return result
    }

}