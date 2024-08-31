package net.maxsmr.feature.address_sorter.data.repository

import android.location.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import net.maxsmr.commonutils.collection.sort.BaseOptionalComparator
import net.maxsmr.commonutils.collection.sort.ISortOption
import net.maxsmr.commonutils.compareFloats
import net.maxsmr.commonutils.compareLongs
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.coroutines.mutableStateIn
import net.maxsmr.core.android.coroutines.usecase.UseCaseResult
import net.maxsmr.core.database.dao.UpsertDao.Companion.NO_ID
import net.maxsmr.core.database.dao.address_sorter.AddressDao
import net.maxsmr.core.database.model.address_sorter.AddressEntity
import net.maxsmr.core.database.model.address_sorter.AddressEntity.Companion.toEntity
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressGeocode
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import net.maxsmr.core.domain.entities.feature.address_sorter.SortPriority

import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository

class AddressRepoImpl(
    private val dao: AddressDao,
    private val cacheRepo: CacheDataStoreRepository,
    private val settingsRepo: SettingsDataStoreRepository,
) : AddressRepo {

    private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("AddressRepoImpl")

    private val ioDispatcher = Dispatchers.IO

    private val scope = CoroutineScope(ioDispatcher + Job())

    override val resultAddresses: MutableStateFlow<List<Address>> = dao.get().map { list ->
        list.map { it.toDomain() }
    }.mutableStateIn(scope, listOf(), ioDispatcher)

    override val upsertCompletedEvent: MutableSharedFlow<Unit> = MutableSharedFlow()

    override suspend fun addItems(items: List<AddressEntity>, rewrite: Boolean) {
        withContext(ioDispatcher) {
            if (items.isNotEmpty()) {
                if (rewrite) {
                    dao.clear()
                } else {
                    val maxSortOrder = dao.getRaw().maxOfOrNull { it.sortOrder } ?: 0
                    items.forEachIndexed { i, item ->
                        item.sortOrder = maxSortOrder + i
                    }
                }
                dao.upsert(items)
            }
        }
    }

    override suspend fun addNewItem(query: String): AddressEntity {
        return withContext(ioDispatcher) {
             updateQuery(null, query)
        }
    }

    override suspend fun getItems(): List<AddressEntity> {
        return withContext(ioDispatcher) {
            dao.getRaw()
        }
    }

    override suspend fun deleteItem(id: Long) {
        withContext(ioDispatcher) {
            dao.deleteById(id)
        }
    }

    override suspend fun updateItem(id: Long, updateFunc: (AddressEntity) -> AddressEntity) {
        withContext(ioDispatcher) {
            val entity = dao.getById(id) ?: return@withContext
            val newEntity = updateFunc(entity)
            if (newEntity.id != entity.id) {
                throw IllegalStateException("AddressEntity id (${newEntity.id}) doesn't match source id (${entity.id})")
            }
            dao.upsert(newEntity)
        }
    }

    override suspend fun clearItems() {
        withContext(ioDispatcher) {
            dao.clear()
        }
    }

    override suspend fun specifyFromSuggest(
        id: Long,
        suggest: AddressSuggest,
        geocodeResult: UseCaseResult<AddressGeocode>,
    ) {
        withContext(ioDispatcher) {
            val entity = dao.getById(id) ?: return@withContext
            val result = suggest.toEntity(
                id,
                entity.sortOrder,
                (geocodeResult as? UseCaseResult.Success)?.data?.location,
                (geocodeResult as? UseCaseResult.Error)?.errorMessage()
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

    override suspend fun upsertItemsWithSort(items: MutableList<AddressEntity>) {
        return withContext(ioDispatcher) {
            if (items.isNotEmpty()) {
                val settings = settingsRepo.getSettings()
                items.sortWith(AddressComparator(settings.sortPriority))
                items.forEachIndexed { index, item ->
                    item.sortOrder = index.toLong()
                }
                items.upsert()
            }
        }
    }

    override suspend fun updateQuery(id: Long?, query: String): AddressEntity {
        return withContext(ioDispatcher) {
            val maxSortOrder = dao.getRaw().maxOfOrNull { it.sortOrder } ?: -1
            val current = if (id != null && id > 0) dao.getById(id) else null
            if (current != null && current.address == query) return@withContext current
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
            newEntity
        }
    }

    override suspend fun setLastLocation(location: Location?) = withContext(ioDispatcher) {
        cacheRepo.setLastLocation(location?.let {
            Address.Location(
                location.latitude.toFloat(),
                location.longitude.toFloat()
            )
        })
    }

    private suspend fun List<AddressEntity>.upsert() {
        // AddressEntity пишем в таблицу и ждём изменения в resultAddresses
        val resultIds = dao.upsert(this)
        if (resultIds.isEmpty() || resultIds.all { it == NO_ID }) {
            // ивент нужен для случая, когда изменений в таблице нет - StateFlow не принимает без изменений
            upsertCompletedEvent.emit(Unit)
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