package net.maxsmr.feature.address_sorter.data.repository

import android.graphics.PointF
import android.location.Location
import com.github.kittinunf.result.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import net.maxsmr.commonutils.collection.sort.BaseOptionalComparator
import net.maxsmr.commonutils.collection.sort.ISortOption
import net.maxsmr.commonutils.compareFloats
import net.maxsmr.commonutils.compareLongs
import net.maxsmr.commonutils.location.distance
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException
import net.maxsmr.commonutils.readString
import net.maxsmr.core.android.coroutines.mutableStateIn
import net.maxsmr.core.database.dao.address_sorter.AddressDao
import net.maxsmr.core.database.model.address_sorter.AddressEntity
import net.maxsmr.core.database.model.address_sorter.AddressEntity.Companion.NO_ID
import net.maxsmr.core.database.model.address_sorter.AddressEntity.Companion.toEntity
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressGeocode
import net.maxsmr.core.domain.entities.feature.address_sorter.AddressSuggest
import net.maxsmr.core.network.api.GeocodeDataSource
import net.maxsmr.core.network.api.SuggestDataSource
import net.maxsmr.core.network.exceptions.EmptyResponseException
import net.maxsmr.core.utils.decodeFromStringOrNull
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import java.io.InputStream
import java.lang.Exception
import java.util.Locale

class AddressRepoImpl(
    private val dao: AddressDao,
    private val cacheRepo: CacheDataStoreRepository,
    private val json: Json,
    private val suggestDataSource: SuggestDataSource,
    private val geocodeDataSource: GeocodeDataSource,
) : AddressRepo {

    private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("AddressRepoImpl")

    private val ioDispatcher = Dispatchers.IO

    private val scope = CoroutineScope(ioDispatcher + Job())

    override val resultAddresses: MutableStateFlow<List<Address>> = dao.get().map { list ->
        list.map { it.toDomain() }
    }.mutableStateIn(scope, listOf(), ioDispatcher)

    override val upsertCompletedEvent: MutableSharedFlow<Unit> = MutableSharedFlow()

    override suspend fun addFromStream(stream: InputStream, rewrite: Boolean): Boolean {
        return withContext(ioDispatcher) {
            stream.readString()?.let { value ->
                json.decodeFromStringOrNull<List<Address>>(value)?.let { list ->
                    val entities = list.map { it.toEntity() }
                    if (entities.isNotEmpty()) {
                        if (rewrite) {
                            dao.clear()
                        } else {
                            val maxSortOrder = dao.getRaw().maxOfOrNull { it.sortOrder } ?: NO_ID
                            entities.forEachIndexed { i, item ->
                                item.sortOrder = maxSortOrder + i
                            }
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

    override suspend fun specifyFromSuggest(id: Long, suggest: AddressSuggest) {
        withContext(ioDispatcher) {
            val current = dao.getById(id) ?: return@withContext
            val geocodeResult: Result<AddressGeocode, Exception>? = if (suggest.location == null) {
                // у Яндекса в ответе suggest нет location - отдельный запрос геокодирования
                try {
                    val geocode = geocodeDataSource.geocode(suggest.address, Locale.getDefault().toString())
                    geocode?.let { Result.success(geocode) } ?: throw EmptyResponseException()
                } catch (e: Exception) {
                    logger.e(formatException(e, "geocode"))
                    Result.error(e)
                }
            } else {
                null
            }
            val result = suggest.toEntity(
                id, current.sortOrder,
                (geocodeResult as? Result.Success)?.value?.location,
                (geocodeResult as? Result.Failure)?.getException(),
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

    override suspend fun sortItems() {
        withContext(ioDispatcher) {
            val entities = dao.getRaw()
            val lastLocation = cacheRepo.getLastLocation()
            val newEntities = entities.map {
                val distance = calculateDistanceByLocation(it.location, lastLocation)
                if (distance != null) {
                    // актуализация пересчитанным валидным значением
                    it.copy(distance = distance)
                } else {
                    it
                }
            }.toMutableList()
            newEntities.sortWith(AddressComparator())
            newEntities.forEachIndexed { index, item ->
                item.sortOrder = index.toLong()
            }
            newEntities.upsert()
        }
    }

    override suspend fun suggest(query: String): List<AddressSuggest> {
        return withContext(ioDispatcher) {
            val lastLocation = cacheRepo.getLastLocation()
            val lang = Locale.getDefault().toString().split("_")[0]
            suggestDataSource.suggest(query, lastLocation, lang = lang)
        }
    }

    override suspend fun suggestWithUpdate(id: Long, query: String): List<AddressSuggest> {
        return withContext(ioDispatcher) {
            // апдейт существующей Entity в таблице при вводе
            updateQuery(id, query)
            suggest(query)
        }
    }

    override suspend fun updateQuery(id: Long?, query: String) {
        withContext(ioDispatcher) {
            val maxSortOrder = dao.getRaw().maxOfOrNull { it.sortOrder } ?: NO_ID
            val current = if (id != null && id > 0) dao.getById(id) else null
            if (current?.address == query) return@withContext
            val newEntity = current?.copy(
                address = query,
                locationException = null,
                distanceException = null
            )?.apply {
                this.id = current.id
                this.sortOrder = current.sortOrder
            } ?: AddressEntity(query).apply {
                sortOrder = maxSortOrder + 1
            }
            dao.upsert(newEntity).also {
                newEntity.id = it
            }
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

    override suspend fun getDistanceByLocation(location: Address.Location?): Float? {
        return withContext(ioDispatcher) {
            calculateDistanceByLocation(location, cacheRepo.getLastLocation())
        }
    }

    private fun List<Address>.addIfNew(item: Address): List<Address> {
        val result = mutableListOf<Address>()
        result.addAll(this)
        if (!result.any { it.id == item.id }) {
            result.add(item)
        }
        return result
    }

    private suspend fun List<AddressEntity>.upsert() {
        // AddressEntity пишем в таблицу и ждём изменения в resultAddresses
        val resultIds = dao.upsert(this)
        if (resultIds.isEmpty() || resultIds.all { it == NO_ID }) {
            // ивент нужен для случая, когда изменений в таблице нет - StateFlow не принимает без изменений
            upsertCompletedEvent.emit(Unit)
        }
    }

    private fun calculateDistanceByLocation(location: Address.Location?, lastLocation: Address.Location?): Float? {
        fun Address.Location.toPointF() = PointF(latitude, longitude)
        val locationPoint = location?.toPointF()
        return locationPoint?.let {
            // сначала пробуем пересчитать относительно имеющихся координат и последней геолокации
            lastLocation?.toPointF()?.let { lastLocation ->
                distance(lastLocation, it)
            }
        }?.takeIf { it >= 0 }
    }

    private class AddressComparator : BaseOptionalComparator<AddressComparator.SortOption, AddressEntity>(
        SortOption.entries.associateWith { true }
    ) {

        override fun compare(lhs: AddressEntity, rhs: AddressEntity, option: SortOption, ascending: Boolean): Int {
            return when (option) {
                SortOption.DISTANCE -> {
                    compareFloats(lhs.distance, rhs.distance, ascending)
                }

                // при совпадении distance, sort_order - следующий критерий
                SortOption.SORT_ORDER -> {
                    compareLongs(lhs.sortOrder, rhs.sortOrder, ascending)
                }
            }
        }

        enum class SortOption : ISortOption {

            DISTANCE,
            SORT_ORDER;

            override val optionName: String = name
        }
    }

}