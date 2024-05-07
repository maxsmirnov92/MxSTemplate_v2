package net.maxsmr.feature.download.data.manager

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.maxsmr.commonutils.GetMode
import net.maxsmr.commonutils.IGetNotifier
import net.maxsmr.commonutils.createFile
import net.maxsmr.commonutils.deleteFile
import net.maxsmr.commonutils.getFiles
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.model.SerializationUtils.fromInputStream
import net.maxsmr.commonutils.model.SerializationUtils.toOutputStream
import net.maxsmr.commonutils.openInputStream
import net.maxsmr.commonutils.openOutputStream
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.coroutines.appendToSet
import net.maxsmr.core.database.model.download.DownloadInfo
import net.maxsmr.core.di.AppDispatchers
import net.maxsmr.core.di.Dispatcher
import net.maxsmr.feature.download.data.DownloadService
import net.maxsmr.feature.download.data.DownloadStateNotifier
import net.maxsmr.feature.download.data.DownloadsRepo
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import java.io.File
import java.io.Serializable
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Обёртка над вызовом [DownloadService], содержащая свою очередь
 */
@Singleton
class DownloadManager @Inject constructor(
    private val downloadsRepo: DownloadsRepo,
    private val settingsRepo: SettingsDataStoreRepository,
    @Dispatcher(AppDispatchers.Default)
    private val defaultDispatcher: CoroutineDispatcher,
    private val notifier: DownloadStateNotifier
) {

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("DownloadManager")

    private val scope =
        CoroutineScope(defaultDispatcher + Job()) // Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private val downloadsPendingStorage = QueueFileStorage("download_pending_queue")

    private val downloadsStorage = QueueFileStorage("download_queue")

    /**
     * Очередь загрузок, ожидающая запуска: итемы удаляются после старта сервиса с любым результатом
     */
    private val downloadsPendingQueue = MutableStateFlow<Set<QueueItem>>(setOf())

    /**
     * Очередь запущенных для отслеживания числа выполняющихся загрузок с остальными в таблице DownloadInfo:
     * сюда итемы попадают после успешного старта сервиса
     */
    private val downloadsLaunchedQueue = MutableStateFlow<Set<QueueItem>>(setOf())

    /**
     * Очередь фактически выполняющихся загрузок: сюда итемы попадают после удаления из [downloadsLaunchedQueue]
     */
    private val downloadsQueue = MutableStateFlow<Set<QueueItem>>(setOf())

    private val _failedStartParamsEvent = MutableSharedFlow<DownloadService.Params>()

    val failedStartParamsEvent: SharedFlow<DownloadService.Params> = _failedStartParamsEvent.asSharedFlow()

    private val _addedToQueueEvent = MutableSharedFlow<DownloadService.Params>()

    val addedToQueueEvent: SharedFlow<DownloadService.Params> = _addedToQueueEvent.asSharedFlow()

    private val idCounter = AtomicInteger(0)

    val downloadsPendingParams by lazy {
        downloadsPendingQueue.map {
            it.map { item -> item.params }
        }
    }

    init {
        scope.launch {
            // убрать из таблицы все "застрявшие" с прошлого раза загрузки
            downloadsRepo.getRaw().forEach {
                if (it.isLoading) {
                    downloadsRepo.remove(it)
                }
            }
            val downloadingItems = downloadsStorage.restoreItems()
            val pendingItems = downloadsPendingStorage.restoreItems()
            // объединение выполняющихся незавершённых и ожидающих с прошлого раза
            val items = downloadingItems + pendingItems
            idCounter.set(items.size)
            logger.i("restored downloadingItems: $downloadingItems")
            logger.i("restored pendingItems: $downloadingItems")
            logger.i("restored total count: ${items.size}")
            downloadsPendingQueue.emit(items.sortedBy { it.id }.toSet())
        }

        scope.launch {
            combine(downloadsRepo.get(), downloadsPendingQueue, downloadsLaunchedQueue, settingsRepo.settings) { v1, v2, v3, _ ->
                DownloadState(v1, v2, v3)
            }.collectLatest {
                refreshQueue()
            }
        }
        scope.launch {
            notifier.downloadStartEvents.collect { info ->
                logger.d("downloadStartEvent: $info")
                val newSet = downloadsLaunchedQueue.value.toMutableSet()

                var startedItem: QueueItem? = null
                val iterator = newSet.iterator()
                while (iterator.hasNext()) {
                    val item = iterator.next()
                    if (item.params == info.params) {
                        if (info.isStarted) {
                            startedItem = item
                        }
                        iterator.remove()
                        break
                    }
                }
                downloadsLaunchedQueue.emit(newSet)

                startedItem?.let {
                    logger.d("$it started, adding to downloadsQueue, removed from downloadsLaunchedQueue")
                    downloadsQueue.appendToSet(it)
                    downloadsStorage.addItem(it)
                }
            }
        }
        scope.launch {
            notifier.downloadStateEvents.collect { state ->
                logger.d("downloadStateEvent: $state")
                if (state !is DownloadStateNotifier.DownloadState.Loading) {
                    // после перехода в завершённое состояние убираем из очереди текущих загрузок
                    val newSet = downloadsQueue.value.toMutableSet()
                    val iterator = newSet.iterator()
                    while (iterator.hasNext()) {
                        val item = iterator.next()
                        if (item.params.targetResourceName == state.oldParams.targetResourceName) {
                            logger.d("$item finished, removing from downloadsQueue")
                            iterator.remove()
                            downloadsStorage.removeItem(item)
                            break
                        }
                    }
                    downloadsQueue.emit(newSet)
                }
            }
        }
        scope.launch {
            notifier.downloadRetryEvents.collect {
                logger.d("downloadRetryEvent: $it")
                enqueueDownloadInternal(it)
            }
        }
    }

    fun enqueueDownload(params: DownloadService.Params) {
        scope.launch { // без ioDispatcher, get room'а всё равно не падает на main...
            enqueueDownloadInternal(params)
        }
    }

    private suspend fun enqueueDownloadInternal(params: DownloadService.Params) {
        logger.i("enqueueDownloadInternal, params: $params")
        if (params.requestParams.url.isEmpty()) return
        if (downloadsLaunchedQueue.value.map { it.params }.contains(params)) {
            logger.w("not added to pending queue - already in launched queue")
            return
        }
        if (downloadsPendingQueue.value.map { it.params }.contains(params)) {
            logger.w("not added to pending queue - already in pending queue")
            return
        }
        // на этапе loading extension не актуализировалось в таблице, можно искать с исходным расширением
        val prevDownload = downloadsRepo.getByNameAndExt(params.resourceNameWithoutExt, params.extension)
        if (prevDownload?.isLoading == true) return
        val item = QueueItem(idCounter.incrementAndGet(), params)
        downloadsPendingQueue.appendToSet(item)
        downloadsPendingStorage.addItem(item)
        _addedToQueueEvent.emit(params)
    }

    /**
     * Запустить сервис по конкретному элементу очереди, убрав его оттуда
     * и добавив в другую ожидающую (при успехе запуска)
     * @param currentDownloads текущие DownloadInfo из таблицы для понимания кол-ва загрузок в данный момент
     */
    private suspend fun refreshQueue() { // state: DownloadState
        logger.d("refreshQueue changed")

        // есть проблемы с актуальностью значений из-за suspend'ов, читаем вручную по месту
        // при этом getRaw() suspend - ставим его на первое место
        val maxDownloadsCount = settingsRepo.settings.firstOrNull()?.maxDownloads ?: MAX_DOWNLOADS_UNLIMITED
        val downloads = downloadsRepo.getRaw() // state.downloads
        val downloadsPendingQueue = downloadsPendingQueue.value // state.downloadsPendingQueue
        val downloadsLaunchedQueue = downloadsLaunchedQueue.value // state.downloadsLaunchedQueue

        val pendingDownloads = downloadsPendingQueue.toMutableSet()
        val launchedDownloads = downloadsLaunchedQueue.toMutableSet()

        fun getLoadingsCount() =
            downloads.count { it.isLoading } + launchedDownloads.count()



        val iterator = pendingDownloads.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (downloadsLaunchedQueue.any { it.id == item.id }) {
                // уже есть в запущенных через start сервиса
                continue
            }
            if (maxDownloadsCount != MAX_DOWNLOADS_UNLIMITED && getLoadingsCount() >= maxDownloadsCount) {
                break
            }
            if (!DownloadService.start(item.params)) {
                // при размере буфера 0 и BufferOverflow.SUSPEND tryEmit не сработает
                logger.e("DownloadService start failed")
//                scope.launch {
                _failedStartParamsEvent.emit(item.params)
//                }
            } else {
                // т.к. статус в таблице после start не успеет смениться на loading сразу
                // (onDownloadStarting в корутине),
                // добавляем в этот Flow
                logger.i("DownloadService start success, adding to downloadsLaunchedQueue")
                launchedDownloads.add(item)
            }
            logger.i("removing from downloadsPendingQueue")
            iterator.remove()
            downloadsPendingStorage.removeItem(item)
        }
        this.downloadsPendingQueue.tryEmit(pendingDownloads)
        this.downloadsLaunchedQueue.tryEmit(launchedDownloads)
    }

    private data class QueueItem(
        val id: Int,
        val params: DownloadService.Params,
    ) : Serializable

    private class QueueFileStorage(
        path: String,
        parentPath: String = baseApplicationContext.filesDir.absolutePath,
    ) {

        val queueDir = File(parentPath, path)

        suspend fun restoreItems(): Set<QueueItem> = suspendCancellableCoroutine { continuation ->

            val notifier = object : IGetNotifier {

                override fun shouldProceed(
                    current: File,
                    collected: Set<File>,
                    currentLevel: Int,
                    wasAdded: Boolean,
                ): Boolean = continuation.isActive
            }
            val files = getFiles(
                queueDir,
                GetMode.FILES, null, 1, notifier
            ).filter { it.name.split(FILE_NAME_PREFIX).size == 2 && it.extension == FILE_NAME_EXT }

            val result: List<Pair<QueueItem, File>> = files.mapNotNull {
                val item = fromInputStream(QueueItem::class.java, it.openInputStream())
                if (item != null) {
                    Pair(item, it)
                } else {
                    null
                }
            }.sortedBy { it.first.id }

            // удалить файлы только по успешно восстановленным
            result.forEach {
                deleteFile(it.second)
            }
            continuation.resume(result.map { it.first }.toSet())
        }

        @Synchronized
        fun addItem(item: QueueItem): Boolean {
            createFile(FILE_NAME_FORMAT.format(item.id), queueDir.absolutePath).openOutputStream()?.let {
                return toOutputStream(item, it)
            }
            return false
        }

        @Synchronized
        fun removeItem(item: QueueItem): Boolean {
            return deleteFile(File(queueDir, FILE_NAME_FORMAT.format(item.id)))
        }

        companion object {

            private const val FILE_NAME_PREFIX = "item_"
            private const val FILE_NAME_EXT = "dat"
            private const val FILE_NAME_FORMAT = "$FILE_NAME_PREFIX%s.$FILE_NAME_EXT"
        }
    }

    private class DownloadState(
        val downloads: List<DownloadInfo>,
        val downloadsPendingQueue: Set<QueueItem>,
        val downloadsLaunchedQueue: Set<QueueItem>,
    ) {

        override fun toString(): String {
            return "DownloadState(downloads=$downloads, downloadsPendingQueue=$downloadsPendingQueue, downloadsLaunchedQueue=$downloadsLaunchedQueue)"
        }
    }

    companion object {

        const val MAX_DOWNLOADS_UNLIMITED = 0
    }
}