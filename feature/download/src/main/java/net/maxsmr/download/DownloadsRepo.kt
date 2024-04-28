package net.maxsmr.download

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.commonutils.states.Status
import net.maxsmr.core.database.dao.download.DownloadsDao
import net.maxsmr.core.database.model.download.DownloadInfo
import net.maxsmr.download.manager.DownloadsHashManager
import net.maxsmr.download.model.IntentSenderParams
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class DownloadsRepo @Inject constructor(private val dao: DownloadsDao) {

    private val intentSenderFlow = MutableStateFlow<VmEvent<IntentSenderParams>?>(null)

    private val notificationRequestCode = AtomicInteger(Random.nextInt(Int.MAX_VALUE / 2))

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

    suspend fun getByName(name: String): DownloadInfo? = dao.getByName(name)

    suspend fun getByNameAndExt(name: String, ext: String): DownloadInfo? = dao.getByNameAndExt(name, ext)

    suspend fun remove(downloadInfo: DownloadInfo) = dao.remove(downloadInfo.id)

    suspend fun removeAll() = dao.removeAll()

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
        if (DownloadsHashManager.checkHash(success.localUri, success.initialHashInfo)) {
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
}