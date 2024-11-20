package net.maxsmr.feature.download.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.commonutils.states.Status
import net.maxsmr.core.database.dao.download.DownloadsDao
import net.maxsmr.core.database.model.download.DownloadInfo
import net.maxsmr.core.network.api.NotificationReaderDataSource
import net.maxsmr.feature.download.data.manager.DownloadsHashManager
import net.maxsmr.feature.download.data.model.IntentSenderParams
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class DownloadsRepo @Inject constructor(
    private val dao: DownloadsDao,
    private val cacheRepo: CacheDataStoreRepository,
    ) {

    private val intentSenderFlow = MutableStateFlow<VmEvent<IntentSenderParams>?>(null)

    private val notificationRequestCode = AtomicInteger(Random.nextInt(Int.MAX_VALUE / 2))

    private val itemIdCounter = AtomicInteger(0)

    fun get() = dao.getAll()

    suspend fun getRaw() = dao.getAllRaw()

    suspend fun upsert(downloadInfo: DownloadInfo) {
        dao.upsert(downloadInfo).also {
            if (it != -1L) {
                // room может вернуть -1
                downloadInfo.id = it
            }
        }
    }

    suspend fun getById(id: Long): DownloadInfo? = dao.getById(id)

    suspend fun getByName(name: String, ext: String): DownloadInfo? = dao.getByName(name, ext)

    suspend fun getByNameAndExt(name: String, ext: String): DownloadInfo? = dao.getByNameAndExt(name, ext)

    suspend fun removeAll() = dao.removeAll()

    suspend fun remove(id: Long) = dao.remove(id)

    suspend fun remove(ids: List<Long>) = dao.remove(*ids.toLongArray())

    suspend fun remove(downloadInfo: DownloadInfo) = remove(downloadInfo.id)

    suspend fun remove(name: String, ext: String) = dao.remove(name, ext)

    suspend fun removeUnfinished() {
        removeIf(false)
//        dao.removeFinished()
    }

    suspend fun removeFinished() {
        removeIf(true)
//        dao.removeFinished()
    }

    private suspend fun removeIf(isFinished: Boolean) {
        val ids = dao.getAllRaw().filter { it.isLoading != isFinished }.map { it.id }
        remove(ids)
    }

    /**
     * Возвращает общий статус загрузок ресурсов в перечне [resourceNames] согласно следующим правилам
     * 1. Если хоть 1 файл еще загружается, либо загрузка не началась -> [Status.LOADING]
     * 1. Иначе если хоть 1 загрузка с ошибкой -> [Status.ERROR]
     * 1. Иначе -> [Status.SUCCESS]
     */
    fun status(resourceNames: List<String>): Flow<Status> =
        downloadsInfos(resourceNames).map {
            when {
                it.isEmpty() || it.any { it.isLoading } -> Status.LOADING
                it.any { it.isError } -> Status.ERROR
                it.all { it.isSuccess } -> Status.SUCCESS
                else -> Status.LOADING
            }
        }

    fun downloadsInfos(resourceNames: List<String>): Flow<List<DownloadInfo>> = dao.getAllByNames(resourceNames)

    /**
     * @return true, если бланк билета уже загружен, иначе false
     */
    suspend fun isDownloaded(resourceName: String, ext: String): Boolean = getDownloaded(resourceName, ext) != null

    suspend fun getDownloaded(resourceName: String, ext: String): DownloadInfo? {
        val downloaded = getByNameAndExt(resourceName, ext) ?: return null
        val success = downloaded.statusAsSuccess ?: return null
        val initialHashInfo = success.initialHashInfo
        if (initialHashInfo != null && DownloadsHashManager.checkHash(success.localUri, initialHashInfo)) {
            return downloaded
        }
        return null
    }

    fun notifyIntentSender(params: IntentSenderParams) {
        intentSenderFlow.tryEmit(VmEvent(params))
    }

    fun getIntentSenderListFiltered(resourceNames: Collection<String>): Flow<VmEvent<IntentSenderParams>?> {
        return intentSenderFlow.filter {
            it?.get(false)?.let { it.resourceName in resourceNames } == true
        }
    }

    fun nextNotificationRequestCode(): Int = notificationRequestCode.incrementAndGet()

    suspend fun setItemIdCounterByItemsCount(count: Int) {
        val counter = if (count == 0) {
            cacheRepo.setLastQueueId(0)
            0
        } else {
            val lastId = cacheRepo.getLastQueueId()
            if (lastId < count) {
                cacheRepo.setLastQueueId(count)
                count
            } else {
                lastId
            }
        }
        itemIdCounter.set(counter)
    }

    suspend fun nextItemId(): Int {
        return itemIdCounter.incrementAndGet().apply {
            cacheRepo.setLastQueueId(this)
        }
    }
}