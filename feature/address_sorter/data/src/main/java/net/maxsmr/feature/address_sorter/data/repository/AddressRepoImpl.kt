package net.maxsmr.feature.address_sorter.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.maxsmr.commonutils.collection.sort.BaseOptionalComparator
import net.maxsmr.commonutils.collection.sort.ISortOption
import net.maxsmr.commonutils.compareFloats
import net.maxsmr.commonutils.compareLongs
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.coroutines.execute.ExecuteResult
import net.maxsmr.core.database.dao.UpsertDao.Companion.NO_ID
import net.maxsmr.core.database.dao.address_sorter.AddressDao
import net.maxsmr.core.database.model.address_sorter.AddressEntity
import net.maxsmr.core.database.model.address_sorter.AddressEntity.Companion.toEntity
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressGeocode
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import net.maxsmr.core.domain.entities.feature.address_sorter.SortPriority

import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository

class AddressRepoImpl(
    private val dao: AddressDao,
    private val settingsRepo: SettingsDataStoreRepository,
) : AddressRepo {

    private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("AddressRepoImpl")

    private val ioDispatcher = Dispatchers.IO

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())

    override val lastAddresses = MutableSharedFlow<List<Address>>(
        // как StateFlow только без distinctUntilChanged
        replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        // transform нет возможности, т.к. нужно эмитить из других методов
        scope.launch {
            dao.get().map { list ->
                list.map { it.toDomain() }
            }.collect {
                lastAddresses.emit(it)
            }
        }
    }

    override suspend fun add(items: List<Address>, rewrite: Boolean) {
        withContext(ioDispatcher) {
            if (items.isNotEmpty()) {
                if (rewrite) {
                    dao.clear()
                } else {
                    val maxSortOrder = dao.getRaw().maxOfOrNull { it.sortOrder } ?: 0
                    items.mapIndexed { index, item -> item.toEntity(index) }.also {
                        it.forEachIndexed { i, item ->
                            item.sortOrder = maxSortOrder + i
                        }
                        dao.upsert(it)
                    }
                }
            }
        }
    }

    override suspend fun addNew(query: String): Address {
        return withContext(ioDispatcher) {
            updateQuery(null, query)
        }
    }

    override suspend fun getAll(): List<Address> {
        return withContext(ioDispatcher) {
            dao.getRaw().map { it.toDomain() }
        }
    }

    override suspend fun get(id: Long): Address? {
        return withContext(ioDispatcher) {
            dao.getById(id)?.toDomain()
        }
    }

    override suspend fun delete(id: Long) {
        withContext(ioDispatcher) {
            dao.deleteById(id)
        }
    }

    override suspend fun update(id: Long, updateFunc: (Address) -> Address) {
        withContext(ioDispatcher) {
            val currentEntity = dao.getById(id) ?: throw IllegalStateException("AddressEntity with id $id not exists")
            val newAddress = updateFunc(currentEntity.toDomain())
            if (newAddress.id != currentEntity.id) {
                throw IllegalStateException("AddressEntity id (${newAddress.id}) doesn't match source id (${currentEntity.id})")
            }
            dao.upsert(newAddress.toEntity(currentEntity.sortOrder))
        }
    }

    override suspend fun clear() {
        withContext(ioDispatcher) {
            dao.clear()
        }
    }

    override suspend fun reload() {
        lastAddresses.emit(getAll())
    }

    override suspend fun specifyFromSuggest(
        id: Long,
        suggest: AddressSuggest,
        geocodeResult: ExecuteResult<AddressGeocode>,
    ) {
        withContext(ioDispatcher) {
            val entity = dao.getById(id) ?: return@withContext
            val result = suggest.toEntity(
                id,
                entity.sortOrder,
                (geocodeResult as? ExecuteResult.Success)?.data?.location,
                (geocodeResult as? ExecuteResult.Error)?.errorMessage()
                    ?.get(baseApplicationContext)?.toString(),
            )
            dao.upsert(result)
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
        result.upsert()
    }

    override suspend fun upsertItemsWithSort(items: MutableList<Address>) {
        return withContext(ioDispatcher) {
            if (items.isNotEmpty()) {
                val entityItems = items.mapIndexed { index, item ->
                    item.toEntity(index)
                }.toMutableList()
                val settings = settingsRepo.getSettings()
                entityItems.sortWith(AddressComparator(settings.sortPriority))
                entityItems.forEachIndexed { index, item ->
                    item.sortOrder = index.toLong()
                }
                entityItems.upsert()
            }
        }
    }

    override suspend fun updateQuery(id: Long?, query: String): Address {
        return withContext(ioDispatcher) {
            val maxSortOrder = dao.getRaw().maxOfOrNull { it.sortOrder } ?: -1
            val current = if (id != null && id > 0) dao.getById(id) else null
            if (current != null && current.address == query) return@withContext current.toDomain()
            val newEntity = current?.copy(
                address = query,
                isSuggested = false,
                locationErrorMessage = null,
                routingErrorMessage = null
            )?.apply {
                this.id = current.id
                this.sortOrder = current.sortOrder
            } ?: AddressEntity(query).apply {
                sortOrder = maxSortOrder + 1
            }
            dao.upsert(newEntity).also {
                newEntity.id = it
            }
            newEntity.toDomain()
        }
    }

    private suspend fun List<AddressEntity>.upsert() {
        // AddressEntity пишем в таблицу и ждём изменения в resultAddresses
        val resultIds = dao.upsert(this)
        if (resultIds.isEmpty() || resultIds.all { it == NO_ID }) {
            // для случая, когда изменений в таблице нет
            reload()
        }
    }

    private class AddressComparator(sortPriority: SortPriority) : BaseOptionalComparator<AddressComparator.SortOption, AddressEntity>(
        if (sortPriority == SortPriority.DISTANCE) {
            listOf(SortOption.DISTANCE, SortOption.DURATION, SortOption.SORT_ORDER)
        } else {
            listOf(SortOption.DURATION, SortOption.DISTANCE, SortOption.SORT_ORDER)
        }.associateWith { true }
    ) {

        override fun compare(lhs: AddressEntity, rhs: AddressEntity, option: SortOption, ascending: Boolean): Int {
            return when (option) {
                SortOption.DISTANCE -> {
                    compareFloats(lhs.distance, rhs.distance, ascending)
                }

                SortOption.DURATION -> {
                    compareLongs(lhs.duration, rhs.duration, ascending)
                }

                // при совпадении distance, sort_order - следующий критерий
                SortOption.SORT_ORDER -> {
                    compareLongs(lhs.sortOrder, rhs.sortOrder, ascending)
                }
            }
        }

        enum class SortOption : ISortOption {

            DISTANCE,
            DURATION,
            SORT_ORDER;

            override val optionName: String = name
        }
    }

}