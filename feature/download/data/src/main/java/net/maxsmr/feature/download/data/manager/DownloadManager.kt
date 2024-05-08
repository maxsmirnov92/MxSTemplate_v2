package net.maxsmr.feature.download.data.manager

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
import net.maxsmr.commonutils.deleteFiles
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
    private val notifier: DownloadStateNotifier,
) {

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("DownloadManager")

    private val scope = CoroutineScope(defaultDispatcher + Job()) // Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    private val downloadsPendingStorage = QueueFileStorage("download_pending_queue")

    private val downloadsStorage = QueueFileStorage("download_queue")

    private val downloadsFinishedStorage = QueueFileStorage("download_finished_queue")

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

    /**
     * Выполненные загрузки для последующего вопспроизведения ивентов с DownloadInfo
     */
    private val downloadsFinishedQueue = MutableStateFlow<Set<QueueItem>>(setOf())

    /**
     * Результирующие элементы, отображающие текущее состояние
     */
    private val _resultItems = MutableStateFlow<List<DownloadInfoResultData>>(listOf())

    val resultItems = _resultItems.asStateFlow()

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

        suspend fun applyFinished(items: MutableSet<QueueItem>) {
            val iterator = items.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                var shouldRemove = true
                item.downloadId?.let {
                    val info = downloadsRepo.getById(it)
                    if (info != null && !info.isLoading) {
                        shouldRemove = false
                        when (info.status) {
                            is DownloadInfo.Status.Success -> {
                                notifier.onDownloadSuccess(info, item.params, item.params)
                            }

                            is DownloadInfo.Status.Error -> {
                                val reason = info.statusAsError?.reason
                                if (reason is CancellationException) {
                                    notifier.onDownloadCancelled(info, item.params, item.params)
                                } else {
                                    notifier.onDownloadFailed(info, item.params, item.params, reason)
                                }
                            }

                            else -> {
                                // Loading'а быть не должно
                            }
                        }

                    }
                }
                if (shouldRemove) {
                    iterator.remove()
                    downloadsFinishedStorage.removeItem(item)
                }
            }
            downloadsFinishedQueue.value = items
        }

        scope.launch {
            // убрать из таблицы все "застрявшие" с прошлого раза загрузки
            downloadsRepo.removeUnfinished()

            val downloadingItems = downloadsStorage.restoreItems(true)
            val pendingItems = downloadsPendingStorage.restoreItems(true)
            val finishedItems = downloadsFinishedStorage.restoreItems(false)
            // объединение выполняющихся незавершённых и ожидающих с прошлого раза
            val items = downloadingItems + pendingItems
            idCounter.set(items.size)
            logger.i("restored downloadingItems: $downloadingItems")
            logger.i("restored pendingItems: $pendingItems")
            logger.i("restored finishedItems: $finishedItems")
            logger.i("restored pending/downloading count: ${items.size}, finished count: ${finishedItems.size}")
            applyFinished(finishedItems.toMutableSet())
            downloadsPendingQueue.emit(items.sortedBy { it.id }.toSet())
        }

        scope.launch {
            combine(
                downloadsRepo.get(),
                downloadsPendingQueue,
                downloadsLaunchedQueue,
                settingsRepo.settings
            ) { v1, v2, v3, _ ->
                DownloadState(v1, v2, v3)
            }.collectLatest {
                refreshQueue()
            }
        }
        scope.launch {
            downloadsFinishedQueue.collectLatest { finishedQueue ->
                // уборать из результирующих итемов те готовые, что отсутствуют в downloadsFinishedQueue;
                // а наполнение _resultItems происходит по ивентам (см. refreshWith)
                val items = _resultItems.value.toMutableList()
                val iterator = items.iterator()
                while (iterator.hasNext()) {
                    val item = iterator.next()
                    if (!item.downloadInfo.isLoading && !finishedQueue.any { it.downloadId == item.id }) {
                        logger.d("item $item is finished and not in finishedQueue - removing from results")
                        iterator.remove()
                    }
                }
                _resultItems.value = items
                // _resultItems.value = _resultItems.value.filter { item -> item.downloadInfo.isLoading }
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
                    if (item.params.targetResourceName == info.params.targetResourceName
                            || item.downloadId == info.downloadInfo?.id
                    ) {
                        if (info.isStarted) {
                            // на этом этапе id загрузки может быть 0, если не была найдена сущность по имени
                            startedItem = item.copy(downloadId = info.downloadInfo?.id)
                        }
                        iterator.remove()
                        break
                    }
                }
                downloadsLaunchedQueue.emit(newSet)

                startedItem?.let {
                    logger.d("item $it started, adding to downloadsQueue, removed from downloadsLaunchedQueue")
                    downloadsQueue.appendToSet(it)
                    downloadsStorage.addItem(it)
                }

                // рефреш результирующих итемов
                if (info.isStarted) {
                    info.downloadInfo?.let { downloadInfo ->
                        DownloadInfoResultData(info.params, downloadInfo, null).refreshWith()
                    }
                }
            }
        }
        scope.launch {
            notifier.downloadStateEvents.collect { state ->
                logger.d("downloadStateEvent: $state")
                if (state !is DownloadStateNotifier.DownloadState.Loading) {
                    val newFinishedSet = downloadsFinishedQueue.value.toMutableSet()
                    // после перехода в завершённое состояние убираем из очереди текущих загрузок
                    val newDownloadsSet = downloadsQueue.value.toMutableSet()
                    val iterator = newDownloadsSet.iterator()
                    while (iterator.hasNext()) {
                        val item = iterator.next()
                        // на этом этапе можно сравнивать только по id в DownloadInfo, если он был правильный;
                        // по targetResourceName в предыдущих неизменённых парамсах остаётся, на случай если id = 0
                        if (item.params.targetResourceName == state.oldParams.targetResourceName
                                || item.downloadId == state.downloadInfo.id
                        ) {
                            logger.d("$item finished, removing from downloadsQueue")
                            iterator.remove()
                            downloadsStorage.removeItem(item)

                            // актуализируем парамсы для завершённого итема
                            val newFinishedItem = item.copy(params = state.params, downloadId = state.downloadInfo.id)
                            var previousFinishedItem: QueueItem? = null
                            val finishedIterator = newFinishedSet.iterator()
                            while (finishedIterator.hasNext()) {
                                val finishedItem = finishedIterator.next()
                                if (finishedItem.downloadId == newFinishedItem.downloadId) {
                                    finishedIterator.remove()
                                    previousFinishedItem = finishedItem
                                    break
                                }
                            }
                            newFinishedSet.add(newFinishedItem)
                            previousFinishedItem?.let {
                                downloadsFinishedStorage.removeItem(previousFinishedItem)
                            }
                            downloadsFinishedStorage.addItem(newFinishedItem)
                            break
                        }
                    }
                    downloadsQueue.emit(newDownloadsSet)
                    downloadsFinishedQueue.emit(newFinishedSet)
                }

                DownloadInfoResultData(state.params, state.downloadInfo, state).refreshWith()
            }
        }
        scope.launch {
            notifier.downloadRetryEvents.collect {
                logger.d("downloadRetryEvent: $it")
                enqueueDownloadInternal(it)
            }
        }
    }

    /**
     * Использовать этот метод вместо [DownloadService.start], если нужна логика с отложенной очередью
     */
    fun enqueueDownload(params: DownloadService.Params) {
        scope.launch { // без ioDispatcher, get room'а всё равно не падает на main...
            enqueueDownloadInternal(params)
        }
    }

    fun removeAllPending() {
        scope.launch {
            downloadsPendingQueue.value = emptySet()
            downloadsPendingStorage.clear()
        }
    }

    fun removeAllFinished(withDb: Boolean = true) {
        scope.launch {
            downloadsFinishedQueue.value = emptySet()
            downloadsFinishedStorage.clear()
            if (withDb) {
                downloadsRepo.removeFinished()
            }
        }
    }

    fun removeFinished(downloadId: Long, withDb: Boolean = true) {
        scope.launch {
            // поиск только в завершённых
            val newFinishedSet = downloadsFinishedQueue.value.toMutableSet()
            val iterator = newFinishedSet.iterator()
            var targetItem: QueueItem? = null
            while (iterator.hasNext()) {
                val item = iterator.next()
                if (item.downloadId == downloadId) {
                    targetItem = item
                    iterator.remove()
                    break
                }
            }
            downloadsFinishedQueue.value = newFinishedSet
            targetItem?.let {
                downloadsFinishedStorage.removeItem(it)
                if (withDb) {
                    downloadsRepo.remove(downloadId)
                }
            }
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
        // или если это retry - params должны быть актуальные!
        val prevDownload = downloadsRepo.getByNameAndExt(params.resourceNameWithoutExt, params.extension)
        if (prevDownload?.isLoading == true) {
            logger.w("DownloadInfo with ${params.targetResourceName} already loading in service")
            return
        }
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
            if (maxDownloadsCount != MAX_DOWNLOADS_UNLIMITED && getLoadingsCount() >= maxDownloadsCount) {
                break
            }
            if (!downloadsLaunchedQueue.any { it.id == item.id }) {
                // пропуск, если уже есть в запущенных через start сервиса
                if (!DownloadService.start(item.params)) {
                    // при размере буфера 0 и BufferOverflow.SUSPEND tryEmit не сработает
                    logger.e("DownloadService start failed with item $item")
                    _failedStartParamsEvent.emit(item.params)
                } else {
                    // т.к. статус в таблице после start не успеет смениться на loading сразу
                    // (onDownloadStarting в корутине),
                    // добавляем в этот Flow
                    logger.i("DownloadService start success, adding item $item to downloadsLaunchedQueue")
                    launchedDownloads.add(item)
                }
            }
            logger.i("removing item $item from downloadsPendingQueue")
            iterator.remove()
            downloadsPendingStorage.removeItem(item)
        }
        this.downloadsPendingQueue.emit(pendingDownloads)
        this.downloadsLaunchedQueue.emit(launchedDownloads)
    }

    private fun DownloadInfoResultData.refreshWith() {
        var has = false
        val currentDownloads = _resultItems.value
        val newDownloads = currentDownloads.map { currentItem ->
            if (currentItem.downloadInfo.id == downloadInfo.id) {
                has = true
                this
            } else {
                currentItem
            }
        }.toMutableList()
        if (!has) {
            newDownloads.add(this)
        }
        _resultItems.value = newDownloads
    }

    /**
     * @param downloadId актуализируется позже, когда первый раз появляется с ивентом DownloadStartInfo
     */
    private data class QueueItem(
        val id: Int,
        val params: DownloadService.Params,
        val downloadId: Long? = null,
    ) : Serializable {

        override fun toString(): String {
            return "QueueItem(id=$id, params=$params, downloadId=$downloadId)"
        }
    }

    private class QueueFileStorage(
        path: String,
        parentPath: String = baseApplicationContext.filesDir.absolutePath,
    ) {

        val queueDir = File(parentPath, path)

        suspend fun restoreItems(shouldDeleteFiles: Boolean): Set<QueueItem> = suspendCancellableCoroutine { continuation ->

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

            if (shouldDeleteFiles) {
                // удалить файлы только по успешно восстановленным
                result.forEach {
                    deleteFile(it.second)
                }
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

        @Synchronized
        fun clear() {
            // FIXME depth = 0
            deleteFiles(queueDir, depth = 1)
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
            return "DownloadState(downloads=$downloads," +
                    "downloadsPendingQueue=$downloadsPendingQueue," +
                    "downloadsLaunchedQueue=$downloadsLaunchedQueue)"
        }
    }

    companion object {

        const val MAX_DOWNLOADS_UNLIMITED = 0
    }
}