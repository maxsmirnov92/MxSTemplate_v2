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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.maxsmr.commonutils.GetMode
import net.maxsmr.commonutils.IGetNotifier
import net.maxsmr.commonutils.REG_EX_FILE_NAME
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
import net.maxsmr.core.android.network.toUrlOrNull
import net.maxsmr.core.database.model.download.DownloadInfo
import net.maxsmr.core.di.AppDispatchers
import net.maxsmr.core.di.Dispatcher
import net.maxsmr.core.network.Method
import net.maxsmr.feature.download.data.DownloadService
import net.maxsmr.feature.download.data.DownloadStateNotifier
import net.maxsmr.feature.download.data.DownloadsRepo
import net.maxsmr.feature.preferences.data.domain.AppSettings
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import java.io.File
import java.io.InterruptedIOException
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
    private val cacheRepo: CacheDataStoreRepository,
    @Dispatcher(AppDispatchers.Default)
    private val defaultDispatcher: CoroutineDispatcher,
    private val notifier: DownloadStateNotifier,
) {

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("DownloadManager")

    private val scope =
        CoroutineScope(defaultDispatcher + Job()) // Executors.newSingleThreadExecutor().asCoroutineDispatcher()

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

    private val _successAddedToQueueEvent = MutableSharedFlow<DownloadService.Params>()

    val successAddedToQueueEvent: SharedFlow<DownloadService.Params> = _successAddedToQueueEvent.asSharedFlow()

    private val _failedAddedToQueueEvent = MutableSharedFlow<Pair<DownloadService.Params, FailAddReason>>()

    val failedAddedToQueueEvent: SharedFlow<Pair<DownloadService.Params, FailAddReason>> =
        _failedAddedToQueueEvent.asSharedFlow()

    private var settings = AppSettings()

    private val idCounter = AtomicInteger(0)

    val downloadsPendingParams by lazy {
        downloadsPendingQueue.map {
            it.map { item -> item.params }
        }
    }

    init {

        suspend fun applyFinished(items: MutableSet<QueueItem>) {
            logger.d("applyFinished")
            val resultItems = _resultItems.value.toMutableList()
            val iterator = items.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                var shouldRemove = true
                item.downloadId?.let {
                    val info = downloadsRepo.getById(it)
                    if (info != null && !info.isLoading) {
                        // такой DownloadInfo числится в таблице, оставляем итем
                        shouldRemove = false

                        val params = item.params
                        val state: DownloadStateNotifier.DownloadState? = when (info.status) {
                            is DownloadInfo.Status.Success -> {
                                DownloadStateNotifier.DownloadState.Success(info, params, params)
                            }

                            is DownloadInfo.Status.Error -> {
                                val reason = info.statusAsError?.reason
                                if (reason is CancellationException || reason is InterruptedIOException) {
                                    DownloadStateNotifier.DownloadState.Cancelled(info, params, params)
                                } else {
                                    DownloadStateNotifier.DownloadState.Failed(reason, info, params, params)
                                }
                            }

                            else -> {
                                // Loading'а быть не должно
                                null
                            }
                        }
                        resultItems.add(DownloadInfoResultData(params, info, state))
                    }
                }
                if (shouldRemove) {
                    iterator.remove()
                    downloadsFinishedStorage.removeItem(item)
                }
            }
            downloadsFinishedQueue.value = items
            _resultItems.value = resultItems
        }

        scope.launch {
            // убрать из таблицы все "застрявшие" с прошлого раза загрузки
            downloadsRepo.removeUnfinished()

            val downloadingItems = downloadsStorage.restoreItems(true)
            val pendingItems = downloadsPendingStorage.restoreItems(true)
            val finishedItems = downloadsFinishedStorage.restoreItems(false)
            // объединение выполняющихся незавершённых и ожидающих с прошлого раза
            val items = downloadingItems + pendingItems
            logger.i("Restored downloadingItems: $downloadingItems")
            logger.i("Restored pendingItems: $pendingItems")
            logger.i("Restored finishedItems: $finishedItems")
            logger.i("Restored pending/downloading count: ${items.size}, finished count: ${finishedItems.size}")
            val totalCount = items.size + finishedItems.size
            val counter = if (totalCount == 0) {
                cacheRepo.setLastQueueId(0)
                0
            } else {
                val lastId = cacheRepo.getLastQueueId()
                if (lastId < totalCount) {
                    cacheRepo.setLastQueueId(totalCount)
                    totalCount
                } else {
                    lastId
                }
            }
            idCounter.set(counter)
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
                        logger.d("$item is finished and not in finishedQueue - removing from results")
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
                    logger.d("$it started, adding to downloadsQueue, removed from downloadsLaunchedQueue")
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
                            newFinishedSet.refreshWith(newFinishedItem) {
                                if (it.downloadId == newFinishedItem.downloadId) {
                                    // если был стартован через retry, такого уже не будет
                                    previousFinishedItem = it
                                    true
                                } else {
                                    false
                                }
                            }.apply {
                                newFinishedSet.clear()
                                newFinishedSet.addAll(this)
                            }

                            previousFinishedItem?.let {
                                downloadsFinishedStorage.removeItem(it)
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
                retryDownloadInternal(it)
            }
        }
        scope.launch {
            settingsRepo.settings.collect {
                settings = it
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

    fun retryDownload(downloadId: Long, params: DownloadService.Params) {
        scope.launch {
            retryDownloadInternal(downloadId, params)
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
            removeFinishedInternal(downloadId, withDb)
        }
    }

    private suspend fun enqueueDownloadInternal(params: DownloadService.Params) {
        logger.i("enqueueDownloadInternal, params: $params")

        fun getActualParams(): DownloadService.Params {
            val settings = settings
            return DownloadService.Params(
                params.requestParams,
                if (settings.disableNotifications) null else params.notificationParams,
                params.resourceName,
                params.storageType,
                params.subDirPath,
                params.targetHashInfo,
                params.skipIfDownloaded,
                params.deleteUnfinished
            )
        }

        val actualParams = getActualParams()

        var failReason: FailAddReason? = null
        try {
            if (!actualParams.validate()) {
                failReason = FailAddReason.NOT_VALID
                return
            }

            val targetResourceName = actualParams.targetResourceName
            if (downloadsLaunchedQueue.value.map { it.params }.any { it.targetResourceName == targetResourceName }) {
                logger.w("Not added to pending queue - \"$targetResourceName\" already in launched queue")
                failReason = FailAddReason.ALREADY_ADDED
                return
            }
            if (downloadsPendingQueue.value.map { it.params }.any { it.targetResourceName == targetResourceName }) {
                logger.w("Not added to pending queue - \"$targetResourceName\" already in pending queue")
                failReason = FailAddReason.ALREADY_ADDED
                return
            }
            // на этапе loading extension не актуализировалось в таблице, можно искать с исходным расширением
            // или если это retry - params должны быть актуальные, но не пройдёт по isLoading
            val prevDownload =
                downloadsRepo.getByNameAndExt(actualParams.resourceNameWithoutExt, actualParams.extension)
            if (prevDownload?.isLoading == true) {
                logger.w("Not added to pending queue - DownloadInfo with \"$targetResourceName\" already loading in service")
                failReason = FailAddReason.ALREADY_LOADING
                return
            }
            val id = idCounter.incrementAndGet().apply {
                cacheRepo.setLastQueueId(this)
            }
            val item = QueueItem(id, actualParams)
            downloadsPendingQueue.appendToSet(item)
            downloadsPendingStorage.addItem(item)

            _successAddedToQueueEvent.emit(actualParams)
        } finally {
            failReason?.let {
                _failedAddedToQueueEvent.emit(Pair(actualParams, it))
            }
        }
    }

    private suspend fun retryDownloadInternal(params: DownloadService.Params) {
        retryDownloadInternal(
            downloadsFinishedQueue.value.find { it.params.targetResourceName == params.targetResourceName }?.downloadId,
            params
        )
    }

    private suspend fun retryDownloadInternal(downloadId: Long?, params: DownloadService.Params) {
        downloadId?.let {
            removeFinishedInternal(it)
        }
        enqueueDownloadInternal(params)
    }

    private suspend fun removeFinishedInternal(downloadId: Long, withDb: Boolean = true) {
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

    /**
     * Запустить сервис по конкретному элементу очереди, убрав его оттуда
     * и добавив в другую ожидающую (при успехе запуска)
     * @param currentDownloads текущие DownloadInfo из таблицы для понимания кол-ва загрузок в данный момент
     */
    private suspend fun refreshQueue() { // state: DownloadState
        logger.d("refreshQueue changed")

        // есть проблемы с актуальностью значений из-за suspend'ов, читаем вручную по месту
        // при этом getRaw() suspend - ставим его на первое место
        val maxDownloadsCount = settings.maxDownloads
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
            logger.i("Removing $item from downloadsPendingQueue")
            iterator.remove()
            downloadsPendingStorage.removeItem(item)
        }
        this.downloadsPendingQueue.emit(pendingDownloads)
        this.downloadsLaunchedQueue.emit(launchedDownloads)
    }

    private fun DownloadInfoResultData.refreshWith() {
        _resultItems.value = _resultItems.value.refreshWith(this) {
            it.downloadInfo.id == this.downloadInfo.id
        }
    }

    /**
     * Рефрешнуть итем в той же позиции новым или добавить его в конец
     */
    private fun <T> Collection<T>.refreshWith(value: T, isSameWithFunc: (T) -> Boolean): List<T> {
        var has = false
        val newCollection = this.map { item ->
            if (isSameWithFunc(item)) {
                has = true
                value
            } else {
                item
            }
        }.toMutableList()
        if (!has) {
            newCollection.add(value)
        }
        return newCollection
    }

    private fun DownloadService.Params.validate(): Boolean {
        if (targetResourceName.isEmpty()) return false
        if (!Regex(REG_EX_FILE_NAME).matches(resourceName)) return false
        if (requestParams.url.toUrlOrNull() == null) return false
        if (requestParams.method == Method.POST.value && requestParams.body == null) return false
        return !requestParams.headers.entries.any { it.key.isEmpty() || it.value.isEmpty() }
    }

    enum class FailAddReason {

        NOT_VALID,
        ALREADY_ADDED,
        ALREADY_LOADING,
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

        suspend fun restoreItems(shouldDeleteFiles: Boolean): Set<QueueItem> =
            suspendCancellableCoroutine { continuation ->

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
            return removeItem(item.id)
        }

        @Synchronized
        private fun removeItem(itemId: Int): Boolean {
            return deleteFile(File(queueDir, FILE_NAME_FORMAT.format(itemId)))
        }

        @Synchronized
        fun clear() {
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