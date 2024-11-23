package net.maxsmr.feature.notification_reader.data

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.MainThread
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.isPackageNameValid
import net.maxsmr.commonutils.isSelfAppInBackground
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.media.readStrings
import net.maxsmr.commonutils.service.StartResult
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.coroutines.tickerFlow
import net.maxsmr.core.android.network.equalsIgnoreSubDomain
import net.maxsmr.core.database.model.notification_reader.NotificationReaderEntity
import net.maxsmr.core.domain.entities.feature.download.DownloadParamsModel
import net.maxsmr.core.domain.entities.feature.settings.AppSettings
import net.maxsmr.core.network.exceptions.NoConnectivityException
import net.maxsmr.core.network.exceptions.NoPreferableConnectivityException
import net.maxsmr.feature.download.data.DownloadService
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.download.data.DownloadsViewModel.Companion.toParams
import net.maxsmr.feature.download.data.manager.DownloadManager
import net.maxsmr.feature.download.data.manager.observeNetworkStateWithSettings
import net.maxsmr.feature.download.data.storage.DownloadServiceStorage
import net.maxsmr.feature.notification_reader.data.usecases.NotificationsSendUseCase
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class NotificationReaderSyncManager @Inject constructor(
    private val notificationReaderRepo: NotificationReaderRepository,
    private val settingsRepo: SettingsDataStoreRepository,
    private val cacheRepo: CacheDataStoreRepository,
    private val downloadManager: DownloadManager,
    private val sendUseCase: NotificationsSendUseCase,
) {

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger(NotificationReaderSyncManager::class.java)

    private val scope = CoroutineScope(Dispatchers.IO)

    private var downloadJob = AtomicReference<Job>()
    private var newWatcherJob = AtomicReference<Job>()
    private var failedWatcherJob = AtomicReference<Job>()
    private var networkStateJob = AtomicReference<Job>()
    private var observeSettingsJob = AtomicReference<Job>()

    private var lastSettings: AppSettings? = null

    private val isPackageListReady = AtomicBoolean(false)
    private val pendingStartService = AtomicBoolean(false)

    init {
        // запуск основной работы без сервиса
        launchMainJob()
    }

    @MainThread
    fun doStart(context: Context, ignoreBackground: Boolean = false): ManagerStartResult {
        logger.d("doStart, ignoreBackground: $ignoreBackground")
        if (ignoreBackground || context.isSelfAppInBackground() == false) {
            if (isNotificationAccessGranted(context)) {
                if (isPackageListReady.get()) {
                    // начиная с Android 8 стартовать foreground сервис из бэкграунда нельзя
                    val result = NotificationReaderListenerService.start(context)
                    return if (result != StartResult.NOT_STARTED_FAILED) {
                        ManagerStartResult.SUCCESS
                    } else {
                        ManagerStartResult.SERVICE_START_FAILED
                    }
                } else {
                    // белый список не актуализирован
                    doLaunchMainJobIfNeeded()
                    return ManagerStartResult.SUCCESS
                }
            } else {
                // начиная с Android 14 стартануть активити находясь в бэкграунде нельзя
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                return ManagerStartResult.SETTINGS_NEEDED
            }
        }
        return ManagerStartResult.NOT_IN_FOREGROUND
    }

    @MainThread
    fun doStop(context: Context, navigateToSettings: Boolean = true): Boolean {
        logger.d("doStop, navigateToSettings: $navigateToSettings")
        downloadJob.cancel()
        newWatcherJob.cancel()
        failedWatcherJob.cancel()
        networkStateJob.cancel()
        observeSettingsJob.cancel()
        // при перезапуске манагера надо перезапросить список
        isPackageListReady.set(false)
        return if (NotificationReaderListenerService.isRunning(context)) {
            if (isNotificationAccessGranted(context)) {
                if (navigateToSettings) {
                    // при наличии доступа сначала отправляем в настройки
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                true
            } else {
                // если доступа уже нет - завершаем через stopService или отправив команду
                NotificationReaderListenerService.stop(context)
            }
        } else {
            true
        }
    }

    @JvmOverloads
    fun doLaunchMainJobIfNeeded(shouldStartService: Boolean = true): Boolean {
        if (downloadJob.get()?.isCancelled != false) {
            // перезапускаем главную при необходимости
            launchMainJob()
            if (shouldStartService) {
                pendingStartService.set(true)
            }
            return true
        }
        return false
    }

    private fun launchMainJob() {
        downloadJob.cancel()
        logger.d("launching downloadJob...")
        // забираем актуальный список пакетов
        downloadJob.set(scope.launch {
            downloadManager.observeDownloadByParams(enqueueDownloadPackageList(), true).collect {
                if (!it.isLoading) {

                    if (it.isSuccessWithData()) {
                        val packageList =
                            it.data?.downloadInfo?.localUri?.readStrings(baseApplicationContext.contentResolver)
                                .orEmpty()
                        cacheRepo.setPackageList(packageList.filter { name ->
                            isPackageNameValid(name) || name == "android"
                        }.toSet())
                    }
                    isPackageListReady.set(true)

                    // после успешной или неуспешной загрузки файла с разрешённым списком
                    // делаем следующее:

                    // 1. сбрасываем статусы у зафейленных или подвешенных и пытаемся их отправить
                    sendOrRemoveNotifications { status is NotificationReaderEntity.Failed || status is NotificationReaderEntity.Loading }

                    // 2. мониторим появляющиеся со статусом "New" для выполнения запроса
                    launchWatcherForNew()
                    // 3. запуск периодического повтора любых зафейленных
                    launchWatcherForFailed()
                    // 4. запуск повтора зафейленных по причине сети при появлении сети / смене настроек
                    launchNetworkStateWatcher()
                    // 5. реагирование на изменение некоторых настроек
                    launchObserveSettingsJob()

                    if (pendingStartService.get()) {
                        if (baseApplicationContext.isSelfAppInBackground() == false
                                || (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || Settings.canDrawOverlays(baseApplicationContext))
                        ) {
                            // отложенный запуск сервиса по готовности белого списка
                            if (isNotificationAccessGranted(baseApplicationContext)) {
                                NotificationReaderListenerService.start(baseApplicationContext)
                            }
                        }
                        pendingStartService.set(false)
                    }

                    downloadJob.cancel()
                }
            }
        })
    }

    private fun launchObserveSettingsJob() {
        if (observeSettingsJob.get()?.isCancelled != false) {
            logger.d("launching observeSettingsJob...")
            observeSettingsJob.set(scope.launch {
                settingsRepo.settingsFlow.collect {
                    if (lastSettings == null) {
                        lastSettings = it
                    }
                    if (lastSettings?.isWhitePackageList != it.isWhitePackageList) {
                        logger.d("setting 'isWhitePackageList' changed to ${it.isWhitePackageList}, refreshing notifications...")
                        sendOrRemoveNotifications { status is NotificationReaderEntity.Failed }
                    }
                    if (lastSettings?.packageListUrl?.toUri()?.equalsIgnoreSubDomain(it.packageListUrl.toUri()) != true) {
                        logger.d("setting 'packageListUrl' changed to ${it.packageListUrl}")
                        doLaunchMainJobIfNeeded(false)
                    }
                    lastSettings = it
                }
            })
        }
    }

    private suspend fun launchWatcherForNew() {
        if (newWatcherJob.get()?.isCancelled != false) {
            logger.d("launching newWatcherJob...")
            newWatcherJob.set(scope.launch {
                notificationReaderRepo.getNotifications { status is NotificationReaderEntity.New }
                    .collect { notifications ->
                        notifications.send()
                    }
            })
        }
    }

    private suspend fun launchWatcherForFailed() {
        failedWatcherJob.cancel()
        val failedWatchInterval = settingsRepo.getSettings().failedNotificationsWatcherInterval
        require(failedWatchInterval >= 0) { "Incorrect failedWatchInterval: $failedWatchInterval" }
        if (failedWatchInterval == 0L) return
        logger.d("launching failedWatcherJob...")
        failedWatcherJob.set(tickerFlow(failedWatchInterval.seconds)
//            .map { LocalDateTime.now() }
            .onEach {
                logger.d("Watcher for failed is running...")
                sendOrRemoveNotifications { status is NotificationReaderEntity.Failed }
            }
            .launchIn(scope))
    }

    private suspend fun launchNetworkStateWatcher() {
        if (networkStateJob.get()?.isCancelled != false) {
            logger.d("launching networkStateJob...")
            networkStateJob.set(
                scope.observeNetworkStateWithSettings(settingsRepo) {
                    if (!it.shouldRetry) return@observeNetworkStateWithSettings
                    val failedNetworkNotifications = notificationReaderRepo.getNotificationsRaw {
                        when (val reason = (status as? NotificationReaderEntity.Failed)?.exception) {
                            is NoConnectivityException, is SocketException, is SocketTimeoutException -> {
                                if (it.loadByWiFiOnly && reason is NoPreferableConnectivityException) {
                                    // поиск зафейленных загрузок по причине отсутствия WiFi, если это соединение появилось
                                    it.connectionInfo.hasWiFi == true
                                } else {
                                    // или по причине любой сети, если она появилась
                                    it.connectionInfo.has
                                }
                            }

                            else -> {
                                false
                            }
                        }
                    }
                    failedNetworkNotifications.sendOrRemove()
                }
            )
        }
    }

    private fun AtomicReference<Job>.cancel() {
        get()?.let {
            if (!it.isCancelled) {
                it.cancel()
            }
            set(null)
        }
    }

    private suspend fun sendOrRemoveNotifications(filterFunc: NotificationReaderEntity.() -> Boolean) {
        val currentNotifications =
            notificationReaderRepo.getNotificationsRaw(filterFunc)
        currentNotifications.sendOrRemove()
    }

    private suspend fun List<NotificationReaderEntity>.sendOrRemove() {
        val newNotifications = mutableListOf<NotificationReaderEntity>()
        val removedNotifications = mutableListOf<NotificationReaderEntity>()
        this.forEach { n ->
            if (cacheRepo.isPackageInList(
                        baseApplicationContext,
                        n.packageName,
                        settingsRepo.getSettings().isWhitePackageList
                    )
            ) {
                // есть в белом/чёрном списке - снова становится "Loading" и отправляется сразу
                newNotifications.add(n.copy(status = NotificationReaderEntity.Loading))
            } else {
                // нет в белом списке - подлежит удалению из таблицы
                removedNotifications.add(n)
            }
        }
        if (newNotifications.isNotEmpty()) {
            newNotifications.send()
        }
        if (removedNotifications.isNotEmpty()) {
            notificationReaderRepo.removeNotifications(removedNotifications)
        }
    }

    private suspend fun List<NotificationReaderEntity>.send() {
        sendUseCase(
            NotificationsSendUseCase.Parameters(
                this,
                if (settingsRepo.getSettings().loadByWiFiOnly) {
                    setOf(NoPreferableConnectivityException.PreferableType.WIFI)
                } else {
                    setOf()
                }
            )
        )
    }

    private suspend fun enqueueDownloadPackageList(): DownloadService.Params {
        val packageListUrl = settingsRepo.getSettings().packageListUrl
        val params = DownloadParamsModel(packageListUrl).toParams()
        DownloadService.Params(
            params.requestParams,
            DownloadService.NotificationParams(
                retryActionIfFailed = false,
                successActions = DownloadsViewModel.defaultSuccessNotificationActions(baseApplicationContext),
            ),
            params.resourceName,
            DownloadServiceStorage.Type.INTERNAL,
            params.subDirPath,
            params.targetHashInfo,
            params.skipIfDownloaded,
            params.replaceFile,
            params.deleteUnfinished,
            params.retryWithNotifier
        ).let {
            downloadManager.enqueueDownload(it)
            return it
        }
    }

    /**
     * @return true, если есть в списке пакетов с доступом к уведомлениям
     */
    private fun isNotificationAccessGranted(context: Context): Boolean {
        // Есть неприятный кейс, когда в списке разрешённых такой package будет,
        // но класс сервиса называется по-другому
        return NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
    }

    enum class ManagerStartResult {
        SUCCESS,
        SERVICE_START_FAILED,
        SETTINGS_NEEDED,
        NOT_IN_FOREGROUND
    }
}