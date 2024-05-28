package net.maxsmr.feature.download.data

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.RecoverableSecurityException
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import net.maxsmr.commonutils.NotificationWrapper
import net.maxsmr.commonutils.NotificationWrapper.Companion.setContentBigText
import net.maxsmr.commonutils.getSerializableExtraCompat
import net.maxsmr.commonutils.getUriFromRawResource
import net.maxsmr.commonutils.isAtLeastOreo
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.formatException
import net.maxsmr.commonutils.media.delete
import net.maxsmr.commonutils.media.getContentName
import net.maxsmr.commonutils.media.getMimeTypeFromName
import net.maxsmr.commonutils.media.lengthOrThrow
import net.maxsmr.commonutils.media.mimeTypeOrThrow
import net.maxsmr.commonutils.media.openInputStreamOrThrow
import net.maxsmr.commonutils.service.createServicePendingIntent
import net.maxsmr.commonutils.service.startNoCheck
import net.maxsmr.commonutils.service.withMutabilityFlag
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.commonutils.toFile
import net.maxsmr.commonutils.wrapChooser
import net.maxsmr.core.ProgressListener
import net.maxsmr.core.android.baseAppName
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.content.FileFormat
import net.maxsmr.core.database.model.download.DownloadInfo
import net.maxsmr.core.di.AppDispatchers
import net.maxsmr.core.di.ApplicationScope
import net.maxsmr.core.di.DI_NAME_MAIN_ACTIVITY_CLASS
import net.maxsmr.core.di.Dispatcher
import net.maxsmr.core.di.DownloaderOkHttpClient
import net.maxsmr.core.domain.entities.feature.download.DownloadParamsModel
import net.maxsmr.core.domain.entities.feature.download.HashInfo
import net.maxsmr.core.domain.entities.feature.network.Method
import net.maxsmr.core.network.ContentDispositionType
import net.maxsmr.core.network.ProgressRequestBody
import net.maxsmr.core.network.ProgressResponseBody
import net.maxsmr.core.network.exceptions.HttpProtocolException.Companion.toHttpProtocolException
import net.maxsmr.core.network.getContentTypeHeader
import net.maxsmr.core.network.getFileNameFromAttachmentHeader
import net.maxsmr.core.network.hasBytesAcceptRanges
import net.maxsmr.core.network.hasContentDisposition
import net.maxsmr.core.network.newCallSuspended
import net.maxsmr.core.network.retrofit.client.okhttp.BaseOkHttpClientManager.Companion.CONNECT_TIMEOUT_DEFAULT
import net.maxsmr.core.network.retrofit.client.okhttp.BaseOkHttpClientManager.Companion.withTimeouts
import net.maxsmr.core.network.writeBufferedOrThrow
import net.maxsmr.feature.download.data.DownloadService.Companion.start
import net.maxsmr.feature.download.data.DownloadService.NotificationParams
import net.maxsmr.feature.download.data.DownloadService.Params
import net.maxsmr.feature.download.data.DownloadStateNotifier.DownloadState.Loading
import net.maxsmr.feature.download.data.manager.DownloadsHashManager
import net.maxsmr.feature.download.data.model.BaseDownloadParams
import net.maxsmr.feature.download.data.model.IntentSenderParams
import net.maxsmr.feature.download.data.storage.DownloadServiceStorage
import net.maxsmr.feature.download.data.storage.file.ShareFile
import net.maxsmr.feature.download.data.storage.file.ViewFile
import net.maxsmr.feature.preferences.data.domain.AppSettings.Companion.UPDATE_NOTIFICATION_INTERVAL_DEFAULT
import net.maxsmr.feature.preferences.data.domain.AppSettings.Companion.UPDATE_NOTIFICATION_INTERVAL_MIN
import net.maxsmr.permissionchecker.PermissionsHelper
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.ByteString
import okio.source
import net.maxsmr.feature.download.data.storage.StoreException
import java.io.*
import java.util.Collections
import javax.inject.Inject
import javax.inject.Named


/**
 * Foreground сервис для загрузки файлов.
 *
 * - Может загружать несколько файлов параллельно
 * - Может загружать в любую заданную область памяти (см. [Params.storageType])
 * - На время работы отображает нотификацию с прогрессом, по итогу загрузки для каждого файла
 * отображает свою нотификацию. В случае успеха загрузки можно указать опциональные действия (см. [NotificationParams.successActions])
 * - Для старта использовать метод [start]
 *
 * Рекомендуется использовать через [DownloadsViewModel], подписавшись при этом на возможные устранимые
 * ошибки [DownloadsViewModel.recoverableExceptions].
 */
@AndroidEntryPoint
class DownloadService : Service() {

    private val context: Context by lazy { this }

    private val groupKeyLoading by lazy { "${context.packageName}.LOADING" }

    private val notificationWrapper: NotificationWrapper by lazy {
        NotificationWrapper(context) {
            permissionsHelper.hasPermissions(
                context,
                PermissionsHelper.addPostNotificationsByApiVersion(emptyList())
            )
        }
    }

    private val notificationChannel by lazy {
        NotificationWrapper.ChannelParams(
            "$packageName.downloads",
            getString(R.string.download_notification_channel_name)
        )
    }

    private val successSoundUri: Uri by lazy {
        getUriFromRawResource(context, R.raw.sound_download_success)
    }

    private val failedSoundUri: Uri by lazy {
        getUriFromRawResource(context, R.raw.sound_download_failed)
    }

    private val successNotificationChannel: NotificationWrapper.ChannelParams by lazy {
        getFinishedNotificationChannel(
            successSoundUri,
            R.string.download_notification_channel_success_name,
            "downloads.success",
            VIBRATION_PATTERN_SUCCESS
        )
    }

    private val failedNotificationChannel: NotificationWrapper.ChannelParams by lazy {
        getFinishedNotificationChannel(
            failedSoundUri,
            R.string.download_notification_channel_failed_name,
            "downloads.failed",
            VIBRATION_PATTERN_FAILED
        )
    }

    private val contextJob by lazy { Job() }

    private val coroutineScope: CoroutineScope by lazy {
        CoroutineScope(ioDispatcher + contextJob)
    }

    /**
     * Мапа текущих загрузок: URL -> парамсы, из которых динамически можно узнать имя
     */
    private val currentDownloads: MutableMap<String, Params> =
        Collections.synchronizedMap(mutableMapOf())

    /**
     * Мапа текущих запущенных Job по загрузке с конкретным id
     */
    private val currentJobs: MutableMap<Long, Job> =
        Collections.synchronizedMap(mutableMapOf())

    private val mainActivityClass: Class<Activity>? by lazy {
        try {
            @Suppress("UNCHECKED_CAST")
            Class.forName(mainActivityClassName) as? Class<Activity>
        } catch (e: Exception) {
            logger.e(formatException(e, "Class.forName"))
            null
        }
    }

    @[Inject DownloaderOkHttpClient]
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var downloadsRepo: DownloadsRepo

    @Inject
    @Dispatcher(AppDispatchers.IO)
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var permissionsHelper: PermissionsHelper

    @Inject
    lateinit var notifier: DownloadStateNotifier

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    @Named(DI_NAME_MAIN_ACTIVITY_CLASS)
    lateinit var mainActivityClassName: String

    override fun onCreate() {
        super.onCreate()
        logger.d("onCreate")
        val notification =
            foregroundNotification(getString(R.string.download_notification_initial_text), null)
        startForeground(DOWNLOADING_NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.d("onStartCommand, intent: $intent, flags: $flags, startId: $startId")
        if (intent?.getBooleanExtra(EXTRA_CANCEL_ALL, false) == true) {
            logger.d("Cancel downloads by user")
            // отмена всех загрузок через родительскую Job
            if (currentJobs.isNotEmpty()) {
                contextJob.cancel()
            } else {
                stopSelf()
            }
            return START_NOT_STICKY
        }
        val cancelDownloadId = intent?.getLongExtra(EXTRA_CANCEL_DOWNLOAD_ID, 0L) ?: 0L
        if (cancelDownloadId > 0) {
            currentJobs.remove(cancelDownloadId)?.let {
                logger.d("Cancel download with id: $cancelDownloadId by user")
                it.cancel()
                return START_NOT_STICKY
            }
        }

        val isForRetry: Boolean = intent?.getLongExtra(EXTRA_NOTIFICATION_ID_RETRY, -1).takeIf { it != -1L }
            ?.let {
                // Убираем нотификацию с ошибкой или отменой загрузки, по которой юзер кликнул "Повторить"
                notificationWrapper.cancel(it.toInt())
                true
            } ?: false

        val params = intent?.getSerializableExtraCompat(EXTRA_DOWNLOAD_SERVICE_PARAMS, Params::class.java)
        if (params == null
                || params.requestParams.url.isEmpty()
                || params.resourceNameWithoutExt.isEmpty()
        ) {

            val reason = params?.let {
                "url or resource name is not specified"
            } ?: "download params not found"
            logger.w("Request ignored: $reason")
            params?.let {
                notifier.onDownloadNotStarted(it)
            }
            stopIfAllLoaded()
            return START_NOT_STICKY
        }

        // защита от множественных параллельных скачиваний одного и того же файла
        if (currentDownloads[params.requestParams.url] != null) {
            notifier.onDownloadNotStarted(params)
            logger.d("Skip download of $params: another same download is already in progress")
            notifier.onDownloadNotStarted(params)
            return START_NOT_STICKY
        } else {
            if (isForRetry && params.retryWithNotifier) {
                // повтор делегируем в другое место
                notifier.onDownloadRetry(params)
//                stopIfAllLoaded()
                return START_NOT_STICKY
            }
            logger.d("Adding params $params to currentDownloads")
            currentDownloads[params.requestParams.url] = params
        }

//        val exceptionHandler = CoroutineExceptionHandler { _, exception ->
//            logger.e(exception)
//            currentDownloads.remove(params.requestParams.url)
//            updateForegroundNotification()
//        }

        // копия параметров до возможного изменения
        val oldParams = Params(
            params.requestParams,
            params.notificationParams,
            params.targetResourceName,
            params.storageType,
            params.subDirPath,
            params.targetHashInfo,
            params.skipIfDownloaded,
            params.deleteUnfinished
        ).apply {
            resourceMimeType = params.resourceMimeType
        }

        var unfinishedLocalUri: Uri? = null

        coroutineScope.launch(/*exceptionHandler*/) { // start = CoroutineStart.LAZY
            // в самом начале mimeType и ext, могут быть неправильными, поиск производим по исходному
            val prevDownload = downloadsRepo.getByNameAndExt(params.resourceNameWithoutExt, params.extension)
            // не актуализируем парамсы из ранее известной DownloadInfo, т.к. mimeType и само расширение могут отличаться
            val storage = DownloadServiceStorage.create(context, params.storageType)

            if (params.skipIfDownloaded) {
                storage.findAlreadyLoaded(params, prevDownload)?.let {
                    logger.d("Skip download of $params: found already loaded file with same hash")
                    onDownloadSuccess(it, params, oldParams)
                    return@launch
                }
            }

            var downloadInfo = prevDownload?.copy(status = DownloadInfo.Status.Loading)
                ?: DownloadInfo(
                    name = params.resourceNameWithoutExt,
                    mimeType = params.resourceMimeType,
                    extension = params.extension,
                    status = DownloadInfo.Status.Loading,
                )
            onDownloadStarting(downloadInfo, params)

            coroutineContext[Job]?.let {
                // запоминаем Job этой загрузки по актуальному id
                currentJobs[downloadInfo.id] = it
            }

            suspend fun onException(e: Exception, localUri: Uri? = null) {
                logger.e("onException: $e, localUri: $localUri")
                val info = downloadInfo.copy(
                    status = DownloadInfo.Status.Error(localUri?.toString(), e)
                )
                if (e is CancellationException || e is InterruptedIOException) {
                    onDownloadCancelled(info, params, oldParams)
                } else {
                    onDownloadFailed(info, params, oldParams, e)
                }
            }

            fun onCancellationException(e: Exception) {
                applicationScope.launch(ioDispatcher) {
                    // после отмены корутины на вызове suspend функций будет брошен эксепшн ->
                    // надо запустить в другом неотменённом скопе
                    onException(e, unfinishedLocalUri)
                }
            }

            class ServiceProgressListener(val type: Loading.Type) : ProgressListener() {

                override val notifyInterval: Long =
                    params.notificationParams?.updateNotificationInterval?.takeIf { it >= UPDATE_NOTIFICATION_INTERVAL_MIN }
                        ?: UPDATE_NOTIFICATION_INTERVAL_DEFAULT

                override fun onProcessing(state: ProgressStateInfo): Boolean {
                    onDownloadProcessing(type, state, downloadInfo, params)
                    return isActive
                }

                override fun notify(currentBytes: Long, totalBytes: Long, done: Boolean, startTime: Long) {
                    super.notify(currentBytes, totalBytes, done, startTime)
                    logger.d("$type: notify, currentBytes=$currentBytes, totalBytes=$totalBytes, done=$done, startTime=$startTime")
                    if (done) {
                        logger.d("$type: done")
                    }
                }
            }

            try {
                val client = okHttpClient.newBuilder().apply {
                    withTimeouts(params.requestParams.connectTimeout)
                }.addNetworkInterceptor {
                    val originalResponse: Response = it.proceed(it.request())
                    originalResponse.newBuilder()
                        .body(originalResponse.body?.let { body ->
                            // индикация загрузки + флаг отмены
                            ProgressResponseBody(body, ServiceProgressListener(Loading.Type.DOWNLOADING))
                        }
                        )
                        .build()
                }.build()

                val response = client.newCallSuspended(
                    params.createRequest(ServiceProgressListener(Loading.Type.UPLOADING)),
                    !params.requestParams.storeErrorBody
                )
                logger.i("Response finished with code ${response.code}, message \"${response.message}\"")

                if (!params.requestParams.storeErrorBody
                        && !params.requestParams.ignoreAttachment
                        && !response.hasContentDisposition(ContentDispositionType.ATTACHMENT)
                        && !response.hasBytesAcceptRanges()
                ) {
                    throw response.toHttpProtocolException("No valid attachment")
                }

                var wasParamsChanged = false

                if (params.resourceMimeType.isEmpty() || !params.requestParams.ignoreContentType) {
                    // при пустом исходном типе или если игнорить заголовок не надо
                    response.getContentTypeHeader().takeIf { it.isNotEmpty() }?.let {
                        if (params.resourceMimeType != it) {
                            // актуализируем из заголовка, если есть - влияет на целевое имя
                            params.resourceMimeType = it
                            wasParamsChanged = true
                        }
                    }
                }
                if (!params.requestParams.ignoreFileName) {
                    response.getFileNameFromAttachmentHeader().takeIf { it.isNotEmpty() }?.let {
                        if (params.targetResourceName != it) {
                            params.resourceName = it
                            wasParamsChanged = true
                        }
                    }
                }

                if (wasParamsChanged) {
                    // апдейт расширения/имени в основной нотификации
                    updateForegroundNotification()
                    // актуализации изменёнными парамсами
                    downloadInfo = downloadInfo.copy(
                        name = params.resourceNameWithoutExt,
                        mimeType = params.resourceMimeType,
                        extension = params.extension,
                    )
//                    downloadsRepo.upsert(downloadInfo)
                }

                val localUri = storage.store(params, prevDownload?.statusAsError?.localUri) { uri, outStream, _ ->
                    unfinishedLocalUri = uri
                    response.writeBufferedOrThrow(outStream)
                }

                val hashInfo = params.targetHashInfo?.takeIf { !it.isEmpty }?.also {
                    // проверка с целевым хэшэм при наличии
                    if (!DownloadsHashManager.checkHash(localUri, it)) {
                        throw StoreException(
                            localUri.toString(),
                            message = "Loaded resource hash doesn't match with expected"
                        )
                    }
                } ?: DownloadsHashManager.getHash(localUri)

                unfinishedLocalUri = null
                val newStatus = DownloadInfo.Status.Success(localUri.toString(), hashInfo)
                onDownloadSuccess(
                    downloadInfo.copy(
                        status = newStatus
                    ), params, oldParams
                )
            } catch (e: CancellationException) {
                onCancellationException(e)
            } catch (e: InterruptedIOException) {
                onCancellationException(e)
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                    downloadsRepo.notifyIntentSender(
                        IntentSenderParams(
                            downloadInfo.id,
                            downloadInfo.localUri,
                            params.resourceNameWithoutExt,
                            e.userAction.actionIntent.intentSender
                        )
                    )
                }
                onException(e, unfinishedLocalUri)
            } catch (e: StoreException) {
                onException(e, e.localUri)
            } catch (e: Exception) {
                onException(e, unfinishedLocalUri)
            } finally {
                if (params.deleteUnfinished) {
                    unfinishedLocalUri?.delete(contentResolver)
                }
                currentJobs.remove(downloadInfo.id)
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun DownloadServiceStorage.findAlreadyLoaded(
        params: Params,
        prevDownload: DownloadInfo?,
    ): DownloadInfo? {

        val success = prevDownload?.statusAsSuccess

        success?.let {
            //Прошлая загрузка успешно завершена, проверяем совпадения хэша по ее uri с целевым хэшем
            // либо с хэшем на момент загрузки (чтобы убедиться, что файл не менялся с тех пор)
            val expectedHash = params.targetHashInfo ?: it.initialHashInfo
            if (!expectedHash.isEmpty && DownloadsHashManager.checkHash(
                        it.localUri,
                        expectedHash
                    )
            ) return prevDownload
        }

        // Пробуем проверить хэши всех существующих uri с совпадающими именами в целевой папки
        val expectedHash = (params.targetHashInfo
            ?: success?.initialHashInfo)?.takeIf { !it.isEmpty }
            ?: return null
        val alreadyLoadedUris = this.alreadyLoadedUris(params)
        return alreadyLoadedUris
            .find { DownloadsHashManager.checkHash(it, expectedHash) }
            ?.let {
                DownloadInfo(
                    id = prevDownload?.id ?: 0,
                    name = params.resourceNameWithoutExt,
                    mimeType = params.resourceMimeType,
                    extension = params.extension,
                    status = DownloadInfo.Status.Success(it.toString(), expectedHash),
                )
            }
    }

    private fun stopIfAllLoaded(): Boolean {
        val empty = currentDownloads.isEmpty()
        if (empty) {
            logger.d("Stop service: no more download tasks")
            stopSelf()
        }
        return empty
    }

    private suspend fun onDownloadStarting(downloadInfo: DownloadInfo, params: Params) {
        logger.d("Download starting: ${downloadInfo.name}")
        downloadsRepo.upsert(downloadInfo)
        updateForegroundNotification()
        notifier.onDownloadStarting(downloadInfo, params)
    }

    private fun onDownloadProcessing(
        type: Loading.Type,
        stateInfo: ProgressListener.ProgressStateInfo,
        downloadInfo: DownloadInfo,
        params: Params,
    ) {
//        logger.d("Download processing: type: $type, $downloadInfo, stateInfo: $stateInfo")
        notifier.onDownloadProcessing(type, stateInfo, downloadInfo, params)

        params.notificationParams ?: return

        val contentTitleResId = when (type) {
            Loading.Type.UPLOADING -> R.string.download_notification_progress_upload_title
            Loading.Type.DOWNLOADING -> R.string.download_notification_progress_download_title
            Loading.Type.STORING -> R.string.download_notification_progress_store_title
        }

        val progress = stateInfo.progressRounded
        notificationWrapper.show(downloadInfo.id.toInt(), notificationChannel) {
            setDefaults(Notification.DEFAULT_ALL)
            setSmallIcon(params.notificationParams.smallIconResId)
            setContentTitle(context.getString(contentTitleResId))
            setProgress(100, progress, progress == 0)
            setContentBigText(params.notificationParams.contentText.takeIf { it.isNotEmpty() }
                ?: params.targetResourceName)
            setGroup(groupKeyLoading)
            setSortKey(SORT_KEY_LOADING)
            setSound(null)
            setSilent(true)

            setOngoing(true)
            setAutoCancel(false)
            addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.download_notification_cancel_button),
                createPendingIntent(
                    context,
                    downloadsRepo.nextNotificationRequestCode(),
                    bundleOf(EXTRA_CANCEL_DOWNLOAD_ID to downloadInfo.id)
                )
            )
            setContentIntent()
        }
    }

    private suspend fun onDownloadSuccess(downloadInfo: DownloadInfo, params: Params, oldParams: Params) {
        val status = downloadInfo.statusAsSuccess ?: return
        logger.d("Download success: $downloadInfo, params: $params, oldParams: $oldParams")

        params.notificationParams?.let { notificationParams ->

            fun Intent.toPendingIntent(): PendingIntent = PendingIntent.getActivity(
                context,
                downloadsRepo.nextNotificationRequestCode(),
                this,
                withMutabilityFlag(FLAG_UPDATE_CURRENT, false)
            )

            val id = downloadInfo.id.toInt()
            notificationWrapper.show(id, successNotificationChannel) {
                setDefaults(Notification.DEFAULT_ALL and Notification.DEFAULT_SOUND.inv())
                setSmallIcon(notificationParams.smallIconResId)
                setContentTitle(notificationParams.successTitle.takeIf { it.isNotEmpty() }
                    ?: getString(R.string.download_notification_success_title))
                setContentBigText(notificationParams.contentText.takeIf { it.isNotEmpty() }
                    ?: params.targetResourceName)
                setGroup(groupKeyLoading)
                setSortKey(SORT_KEY_FINISHED)
                setSound(successSoundUri)
                setVibrate(VIBRATION_PATTERN_SUCCESS)
                setOngoing(false)
                setAutoCancel(true)
                val uri = status.localUri
                // кнопки показываем при успешной загрузке (нет exception, урла есть)
                val mimeType = downloadInfo.mimeType
                notificationParams.actionIntent<NotificationParams.SuccessAction.View>()?.let {
                    addAction(
                        it.iconResId,
                        it.notificationActionName.takeIf { it.isNotEmpty() }
                            ?: getString(R.string.download_notification_success_view_button),
                        it.intent(uri, mimeType).toPendingIntent()
                    )
                }
                notificationParams.actionIntent<NotificationParams.SuccessAction.Share>()?.let {
                    addAction(
                        it.iconResId,
                        it.notificationActionName.takeIf { it.isNotEmpty() }
                            ?: getString(R.string.download_notification_success_share_button),
                        it.intent(uri, mimeType).toPendingIntent()
                    )
                }
                setContentIntent()
            }
        }

        downloadsRepo.upsert(downloadInfo)
        currentDownloads.remove(params.requestParams.url)
        updateForegroundNotification()

        notifier.onDownloadSuccess(downloadInfo, params, oldParams)
    }

    private suspend fun onDownloadFailed(
        downloadInfo: DownloadInfo,
        params: Params,
        oldParams: Params,
        e: Exception?,
    ) {
        logger.e("Download failed: $downloadInfo, params: $params, oldParams: $oldParams", e)

        params.notificationParams?.let { notificationParams ->
            val id = downloadInfo.id.toInt()
            notificationWrapper.show(id, failedNotificationChannel) {
                setDefaults(Notification.DEFAULT_ALL and Notification.DEFAULT_SOUND.inv())
                setSmallIcon(notificationParams.smallIconResId)
                setContentTitle(notificationParams.errorTitle.takeIf { it.isNotEmpty() }
                    ?: getString(R.string.download_notification_failed_title))
                setContentBigText(notificationParams.contentText.takeIf { it.isNotEmpty() }
                    ?: params.targetResourceName)
                setGroup(groupKeyLoading)
                setSortKey(SORT_KEY_FINISHED)
                setSound(failedSoundUri)
                setVibrate(VIBRATION_PATTERN_FAILED)
                setOngoing(false)
                addRetryAction(downloadInfo.id, params)
                setContentIntent()
            }
        }

        downloadsRepo.upsert(downloadInfo)
        currentDownloads.remove(params.requestParams.url)
        updateForegroundNotification()

        notifier.onDownloadFailed(downloadInfo, params, oldParams, e)
    }

    private suspend fun onDownloadCancelled(downloadInfo: DownloadInfo, params: Params, oldParams: Params) {
        logger.w("Download cancelled by user: $downloadInfo, params: $params, oldParams: $oldParams")

        params.notificationParams?.let { notificationParams ->
            val id = downloadInfo.id.toInt()
            notificationWrapper.show(id, failedNotificationChannel) {
                setDefaults(Notification.DEFAULT_ALL and Notification.DEFAULT_SOUND.inv())
                setSmallIcon(notificationParams.smallIconResId)
                setContentTitle(notificationParams.cancelTitle.takeIf { it.isNotEmpty() }
                    ?: getString(R.string.download_notification_cancelled_title))
                setContentBigText(notificationParams.contentText.takeIf { it.isNotEmpty() }
                    ?: params.targetResourceName)
                setGroup(groupKeyLoading)
                setSortKey(SORT_KEY_FINISHED)
                setOngoing(false)
                addRetryAction(downloadInfo.id, params)
            }
        }

        currentDownloads.remove(params.requestParams.url)
//        downloadsRepo.remove(downloadInfo)
        downloadsRepo.upsert(downloadInfo)
        updateForegroundNotification()

        notifier.onDownloadCancelled(downloadInfo, params, oldParams)
    }

    private fun NotificationCompat.Builder.addRetryAction(downloadId: Long, params: Params) {
        addAction(
            android.R.drawable.stat_notify_sync,
            getString(R.string.download_notification_error_retry_button),
            createPendingIntent(
                context,
                downloadsRepo.nextNotificationRequestCode(),
                bundleOf(
                    EXTRA_DOWNLOAD_SERVICE_PARAMS to params,
                    EXTRA_NOTIFICATION_ID_RETRY to downloadId
                )
            )
        )
    }

    private fun updateForegroundNotification() {
        if (stopIfAllLoaded()) return
        val (message, size) = currentDownloads.values.joinToString { it.targetResourceName } to currentDownloads.size

        val title = context.resources.getQuantityString(R.plurals.download_files, size, size)
        notificationWrapper.show(
            DOWNLOADING_NOTIFICATION_ID,
            foregroundNotification(title, message)
        )
    }

    private fun foregroundNotification(title: String, message: String?): Notification {
        return notificationWrapper.create(notificationChannel) {
            setDefaults(Notification.DEFAULT_ALL)
            setSmallIcon(R.drawable.ic_download)
            setContentTitle(title)
            setContentText(message)
            setProgress(0, 0, true)
            setOnlyAlertOnce(true)
            setGroup(groupKeyLoading)
            setSortKey(SORT_KEY_LOADING_ALL)
            setSound(null)
            setSilent(true)
            addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.download_notification_cancel_button),
                createPendingIntent(
                    context,
                    downloadsRepo.nextNotificationRequestCode(),
                    bundleOf(EXTRA_CANCEL_ALL to true)
                )
            )
            setContentIntent()
        }
    }

    private fun getFinishedNotificationChannel(
        soundUri: Uri,
        @StringRes titleResId: Int,
        idPostfix: String,
        vibrationPattern: LongArray,
    ): NotificationWrapper.ChannelParams {
        return NotificationWrapper.ChannelParams(
            "$packageName.$idPostfix",
            getString(titleResId)
        ) {
            if (isAtLeastOreo()) {
                val att: AudioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(soundUri, att)
                enableLights(true)
                this.vibrationPattern = vibrationPattern
            }
        }
    }

    private fun NotificationCompat.Builder.setContentIntent() {
        mainActivityClass?.let {
            setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, it),
                    withMutabilityFlag(FLAG_UPDATE_CURRENT, false)
                )
            )
        }
    }

    override fun onDestroy() {
        coroutineScope.coroutineContext.cancel()
        logger.d("onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * @param requestParams параметры http запроса для получения ресурса (файла)
     * @param resourceName имя ресурса, без расширения (при его наличии будет использовано как вспомогательное далее)
     * @param storageType определяет корневую директорию для сохранения ресурса
     * @param subDirPath путь к подпапке относительно [storageType], куда требуется поместить скаченный
     * ресурс. При Null помещается в корень [storageType].
     * @param targetHashInfo целевой (ожидаемый) хэш файла, либо null, если заранее неизвестен
     * @param skipIfDownloaded пропуск загрузки, если ресурс уже был успешно загружен ранее и его хэш совпадает
     * @param notificationParams параметры показа нотификаций и действий к ним; null, если нотификации по каждой загрузке не нужны
     * @param retryWithNotifier если true, повтор загрузки по действию в нотификации будет происходить через [DownloadStateNotifier]
     */
    class Params @JvmOverloads constructor(
        val requestParams: RequestParams,
        val notificationParams: NotificationParams?,
        resourceName: String,
        val storageType: DownloadServiceStorage.Type = DownloadServiceStorage.Type.SHARED,
        val subDirPath: String? = null,
        val targetHashInfo: HashInfo? = null,
        val skipIfDownloaded: Boolean = true,
        val replaceFile: Boolean = false,
        val deleteUnfinished: Boolean = true,
        val retryWithNotifier: Boolean = true,
    ) : BaseDownloadParams(requestParams.url, resourceName) {

        constructor(params: Params) : this(
            params.requestParams,
            params.notificationParams,
            params.resourceName,
            params.storageType,
            params.subDirPath,
            params.targetHashInfo,
            params.skipIfDownloaded,
            params.deleteUnfinished
        )

        fun createRequest(listener: ProgressListener): Request = requestParams.createRequest(listener)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Params) return false
            if (!super.equals(other)) return false

            if (requestParams != other.requestParams) return false
            if (notificationParams != other.notificationParams) return false
            if (storageType != other.storageType) return false
            if (subDirPath != other.subDirPath) return false
            if (targetHashInfo != other.targetHashInfo) return false
            if (skipIfDownloaded != other.skipIfDownloaded) return false
            return deleteUnfinished == other.deleteUnfinished
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + requestParams.hashCode()
            result = 31 * result + notificationParams.hashCode()
            result = 31 * result + storageType.hashCode()
            result = 31 * result + (subDirPath?.hashCode() ?: 0)
            result = 31 * result + (targetHashInfo?.hashCode() ?: 0)
            result = 31 * result + skipIfDownloaded.hashCode()
            result = 31 * result + deleteUnfinished.hashCode()
            return result
        }

        companion object {

            @JvmOverloads
            fun defaultPOSTServiceParamsFor(
                uri: String,
                preferredFileName: String?,
                body: RequestParams.Body,
                ignoreContentType: Boolean = false,
                ignoreAttachment: Boolean = false,
                ignoreFileName: Boolean = true,
                storeErrorBody: Boolean = false,
                connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
                headers: HashMap<String, String> = HashMap(),
                format: FileFormat? = null,
                subDir: String = EMPTY_STRING,
                targetHashInfo: HashInfo? = null,
                replaceFile: Boolean = false,
                deleteUnfinished: Boolean = true,
                notificationParams: NotificationParams,
            ): Params {
                val baseSubDir = baseAppName
                val fileName: String
                val targetIgnoreFileName: Boolean
                if (preferredFileName.isNullOrEmpty()) {
                    // желаемое не указано - подбираем из урлы,
                    // но не игнорируем после получения ответа в заголовке
                    fileName = getContentName(preferredFileName.orEmpty(), uri)
                    targetIgnoreFileName = false
                } else {
                    fileName = preferredFileName
                    targetIgnoreFileName = ignoreFileName
                }
                return Params(
                    RequestParams.newPost(
                        uri,
                        body,
                        headers = headers,
                        ignoreContentType = ignoreContentType,
                        ignoreAttachment = ignoreAttachment,
                        ignoreFileName = targetIgnoreFileName,
                        storeErrorBody = storeErrorBody,
                        connectTimeout = connectTimeout
                    ),
                    notificationParams,
                    fileName,
                    DownloadServiceStorage.Type.SHARED,
                    if (subDir.isEmpty()) baseSubDir else toFile(subDir, baseSubDir)?.absolutePath,
                    targetHashInfo,
                    targetHashInfo != null,
                    replaceFile = replaceFile,
                    deleteUnfinished = deleteUnfinished
                ).apply {
                    resourceMimeType = format?.let {
                        format.mimeType
                    } ?: run {
                        getMimeTypeFromName(fileName)
                    }
                }
            }

            @JvmOverloads
            fun defaultGETServiceParamsFor(
                uri: String,
                preferredFileName: String?,
                ignoreContentType: Boolean = false,
                ignoreAttachment: Boolean = false,
                ignoreFileName: Boolean = true,
                storeErrorBody: Boolean = false,
                connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
                headers: HashMap<String, String> = HashMap(),
                format: FileFormat? = null,
                subDir: String = EMPTY_STRING,
                targetHashInfo: HashInfo? = null,
                replaceFile: Boolean = false,
                deleteUnfinished: Boolean = true,
                notificationParams: NotificationParams,
            ): Params {
                val baseSubDir = baseAppName
                val fileName: String
                val targetIgnoreFileName: Boolean
                if (preferredFileName.isNullOrEmpty()) {
                    // желаемое не указано - подбираем из урлы,
                    // но не игнорируем после получения ответа в заголовке
                    fileName = getContentName(preferredFileName.orEmpty(), uri)
                    targetIgnoreFileName = false
                } else {
                    fileName = preferredFileName
                    targetIgnoreFileName = ignoreFileName
                }
                return Params(
                    RequestParams.newGet(
                        uri,
                        headers = headers,
                        ignoreContentType = ignoreContentType,
                        ignoreAttachment = ignoreAttachment,
                        ignoreFileName = targetIgnoreFileName,
                        storeErrorBody = storeErrorBody,
                        connectTimeout = connectTimeout
                    ),
                    notificationParams,
                    fileName,
                    DownloadServiceStorage.Type.SHARED,
                    if (subDir.isEmpty()) baseSubDir else toFile(subDir, baseSubDir)?.absolutePath,
                    targetHashInfo,
                    targetHashInfo != null,
                    replaceFile = replaceFile,
                    deleteUnfinished = deleteUnfinished
                ).apply {
                    resourceMimeType = format?.let {
                        format.mimeType
                    } ?: run {
                        getMimeTypeFromName(fileName)
                    }
                }
            }

        }
    }

    data class RequestParams(
        val url: String,
        val method: String,
        val headers: HashMap<String, String>,
        val body: Body?,
        val ignoreContentType: Boolean,
        val ignoreAttachment: Boolean,
        val ignoreFileName: Boolean,
        val storeErrorBody: Boolean,
        val connectTimeout: Long,
    ) : Serializable {

        fun createRequest(listener: ProgressListener): Request = Request.Builder()
            .url(url)
            .method(method, body?.takeIf { !it.isEmpty }?.createRequestBody()?.let {
                ProgressRequestBody(it, listener)
            })
            .headers(headers.toHeaders())
            .build()

        override fun toString(): String {
            return "RequestParams(url='$url', " +
                    "method='$method', " +
                    "headers=$headers, " +
                    "body=$body, " +
                    "ignoreContentType=$ignoreContentType, " +
                    "ignoreAttachment=$ignoreAttachment, " +
                    "ignoreFileName=$ignoreFileName, " +
                    "storeErrorBody=$storeErrorBody, " +
                    "connectTimeout=$connectTimeout)"
        }

        class Body(val content: Serializable, val mimeType: String? = null) : Serializable {

            val isEmpty: Boolean = when (content) {
                is Uri -> content.value.isEmpty()
                is File -> content.length() == 0L
                is ByteArray -> content.isEmpty()
                is String -> content.isEmpty()
                is ByteString -> content.size == 0
                else -> true
            }

            fun createRequestBody(): RequestBody {
                val type = mimeType?.toMediaTypeOrNull()
                return when (content) {
                    is Uri -> ContentOrFileUriRequestBody(content.toUri(), mimeType)
                    is File -> content.asRequestBody(
                        type
                            ?: getMimeTypeFromName(content.name).toMediaTypeOrNull()
                    )

                    is ByteArray -> content.toRequestBody(type)
                    is String -> content.toRequestBody(type)
                    is ByteString -> content.toRequestBody(type)
                    else -> throw IllegalArgumentException("Incorrect body type: " + content.javaClass)
                }
            }

            data class Uri(
                val value: String,
            ) : Serializable {

                fun toUri() = android.net.Uri.parse(value)
            }
        }

        companion object {

            @JvmStatic
            @JvmOverloads
            fun newPost(
                url: String,
                body: Body,
                headers: HashMap<String, String> = HashMap(),
                ignoreContentType: Boolean = true,
                ignoreAttachment: Boolean = false,
                ignoreFileName: Boolean = false,
                storeErrorBody: Boolean = false,
                connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
            ): RequestParams {
                return RequestParams(
                    url,
                    Method.POST.value,
                    headers,
                    body,
                    ignoreContentType,
                    ignoreAttachment,
                    ignoreFileName,
                    storeErrorBody,
                    connectTimeout
                )
            }

            @JvmStatic
            @JvmOverloads
            fun newGet(
                url: String,
                headers: HashMap<String, String> = HashMap(),
                ignoreContentType: Boolean = true,
                ignoreAttachment: Boolean = false,
                ignoreFileName: Boolean = false,
                storeErrorBody: Boolean = false,
                connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
                appendGetParams: ((Uri) -> Uri)? = null,
            ): RequestParams {
                val targetUrl = if (appendGetParams != null) {
                    Uri.parse(url)?.let { appendGetParams(it) }?.toString() ?: url
                } else {
                    url
                }
                return RequestParams(
                    targetUrl,
                    Method.GET.value,
                    headers,
                    null,
                    ignoreContentType,
                    ignoreAttachment,
                    ignoreFileName,
                    storeErrorBody,
                    connectTimeout
                )
            }
        }
    }

    /**
     * @param successActions возможные действия при успехе загрузки
     */
    data class NotificationParams @JvmOverloads constructor(
        @DrawableRes val smallIconResId: Int = R.drawable.ic_download,
        val successTitle: String = EMPTY_STRING,
        val errorTitle: String = EMPTY_STRING,
        val cancelTitle: String = EMPTY_STRING,
        val contentText: String = EMPTY_STRING,
        val updateNotificationInterval: Long = UPDATE_NOTIFICATION_INTERVAL_DEFAULT,
        val successActions: MutableSet<SuccessAction> = mutableSetOf(),
    ) : Serializable {

//        val successSoundUri: Uri? get() = successSoundResId?.let { baseApplicationContext.rawResourceToUri(it) }
//
//        val errorSoundUri: Uri? get() = errorSoundResId?.let { baseApplicationContext.rawResourceToUri(it) }

        override fun toString(): String {
            return "NotificationParams(actions=$successActions)"
        }

        fun SuccessAction.add() {
            if (!successActions.add(this)) {
                successActions.remove(this)
                successActions.add(this)
            }
        }

        inline fun <reified T : SuccessAction> actionIntent(): SuccessAction? {
            return successActions.filterIsInstance<T>().firstOrNull()
        }

        sealed class SuccessAction : Serializable {

            abstract val id: Int
            abstract val chooserTitle: String

            @get:DrawableRes
            abstract val iconResId: Int

            abstract val notificationActionName: String

            fun intent(uri: Uri, mimeType: String): Intent {
                logger.d("Success mimeType: $mimeType, action: $this")
                return createIntent(uri, mimeType).wrapChooser(chooserTitle)
            }

            protected abstract fun createIntent(uri: Uri, mimeType: String): Intent

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is SuccessAction) return false

                return id == other.id
            }

            override fun hashCode(): Int {
                return id
            }

            data class View @JvmOverloads constructor(
                override val chooserTitle: String,
                override val notificationActionName: String = EMPTY_STRING,
                override val iconResId: Int = android.R.drawable.ic_menu_view,
            ) : SuccessAction() {

                override val id: Int = 1

                override fun createIntent(uri: Uri, mimeType: String): Intent = ViewFile.intent(
                    ViewFile.Data(uri, mimeType)
                )
            }

            data class Share @JvmOverloads constructor(
                override val chooserTitle: String,
                override val notificationActionName: String = EMPTY_STRING,
                override val iconResId: Int = android.R.drawable.ic_menu_share,
                val intentSubject: String = EMPTY_STRING,
                val intentText: String = EMPTY_STRING,
                val intentEmails: ArrayList<String> = arrayListOf(),
            ) : SuccessAction() {

                override val id: Int = 2

                override fun createIntent(uri: Uri, mimeType: String): Intent = ShareFile.intent(
                    ShareFile.Data(uri, mimeType, intentText, intentSubject, intentEmails)
                )
            }
        }
    }

    private class ContentOrFileUriRequestBody(
        private val uri: Uri,
        private val type: String? = null,
    ) : RequestBody() {

        override fun contentType(): MediaType? {
            val contentType = type ?: uri.mimeTypeOrThrow(baseApplicationContext.contentResolver)
            return contentType.toMediaTypeOrNull()
        }

        override fun contentLength(): Long = uri.lengthOrThrow(baseApplicationContext.contentResolver)

        @Throws(IOException::class)
        override fun writeTo(sink: BufferedSink) {
            val inputStream = uri.openInputStreamOrThrow(baseApplicationContext.contentResolver)
            inputStream.source().use { source ->
                sink.writeAll(source)
            }
        }
    }

    companion object {

        const val EXTRA_NOTIFICATION_ID_RETRY = "download_service_notification_id_retry"
        const val EXTRA_DOWNLOAD_SERVICE_PARAMS = "download_service_params"
        const val EXTRA_CANCEL_ALL = "download_service_cancel_all"
        const val EXTRA_CANCEL_DOWNLOAD_ID = "download_service_cancel_download_id"

        /**
         * Ид уведомления текущей загрузки. Для корректной работы не должен пересекаться с ид успешных загрузок/фейлов
         */
        private const val DOWNLOADING_NOTIFICATION_ID = -1

        //лексиграфическая сортировка нотификаций - уведомление текущей загрузки всегда сверху
        private const val SORT_KEY_LOADING_ALL = "A"
        private const val SORT_KEY_LOADING = "B"
        private const val SORT_KEY_FINISHED = "C"

        val VIBRATION_PATTERN_SUCCESS = longArrayOf(100L, 100L)
        val VIBRATION_PATTERN_FAILED = longArrayOf(1000L)

        private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("DownloadService")

        /**
         * Стартует сервис для загрузки 1 файла
         */
        @JvmStatic
        @JvmOverloads
        fun start(params: Params, context: Context = baseApplicationContext): Boolean {
            return startNoCheck(
                context,
                DownloadService::class.java,
                bundleOf(EXTRA_DOWNLOAD_SERVICE_PARAMS to params),
                startForeground = true
            )
        }

        @JvmStatic
        @JvmOverloads
        fun cancel(id: Long, context: Context = baseApplicationContext): Boolean {
            return startNoCheck(
                context,
                DownloadService::class.java,
                bundleOf(EXTRA_CANCEL_DOWNLOAD_ID to id),
                startForeground = true
            )
        }

        @JvmStatic
        @JvmOverloads
        fun cancelAll(context: Context = baseApplicationContext): Boolean {
            return startNoCheck(
                context,
                DownloadService::class.java,
                bundleOf(EXTRA_CANCEL_ALL to true),
                startForeground = true
            )
        }

        @JvmStatic
        @JvmOverloads
        fun getViewAction(
            context: Context = baseApplicationContext,
            @StringRes chooserTitleRes: Int = R.string.view_choose_client_file_title,
            @StringRes notificationActionRes: Int = R.string.download_notification_success_view_button,
            @DrawableRes iconResId: Int = android.R.drawable.ic_menu_view,
        ) = NotificationParams.SuccessAction.View(
            context.getString(chooserTitleRes),
            context.getString(notificationActionRes),
            iconResId
        )

        @JvmStatic
        @JvmOverloads
        fun getShareAction(
            context: Context = baseApplicationContext,
            @StringRes chooserTitleRes: Int = R.string.view_choose_client_file_title,
            @StringRes notificationActionRes: Int = R.string.download_notification_success_share_button,
            @DrawableRes iconResId: Int = android.R.drawable.ic_menu_share,
            intentSubject: String = EMPTY_STRING,
            intentText: String = EMPTY_STRING,
            intentEmails: ArrayList<String> = arrayListOf(),
        ) = NotificationParams.SuccessAction.Share(
            context.getString(chooserTitleRes),
            context.getString(notificationActionRes),
            iconResId,
            intentSubject,
            intentText,
            intentEmails
        )

        private fun createPendingIntent(
            context: Context,
            requestCode: Int,
            bundle: Bundle,
        ): PendingIntent = createServicePendingIntent(
            context,
            DownloadService::class.java,
            requestCode,
            PendingIntent.FLAG_CANCEL_CURRENT,
            true,
            args = bundle
        )
    }
}