package net.maxsmr.feature.download.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.maxsmr.commonutils.ALGORITHM_SHA1
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.gui.message.TextMessageException
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.commonutils.live.mapNotNull
import net.maxsmr.commonutils.live.unsubscribeIf
import net.maxsmr.commonutils.media.readString
import net.maxsmr.commonutils.media.takePersistableReadPermission
import net.maxsmr.commonutils.states.ILoadState.Companion.copyOf
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.commonutils.states.Status
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.base.actions.SnackbarAction
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.content.FileFormat
import net.maxsmr.core.database.model.download.DownloadInfo
import net.maxsmr.core.di.AppDispatchers
import net.maxsmr.core.di.BaseJson
import net.maxsmr.core.di.Dispatcher
import net.maxsmr.core.domain.entities.feature.download.DownloadParamsModel
import net.maxsmr.core.domain.entities.feature.download.HashInfo
import net.maxsmr.core.domain.entities.feature.network.Method
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.asOkDialog
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.core.utils.decodeFromStringOrNull
import net.maxsmr.feature.download.data.DownloadService.Params.Companion.defaultGETServiceParamsFor
import net.maxsmr.feature.download.data.DownloadService.Params.Companion.defaultPOSTServiceParamsFor
import net.maxsmr.feature.download.data.manager.DownloadManager
import net.maxsmr.feature.download.data.manager.DownloadManager.FailAddReason
import net.maxsmr.feature.download.data.model.IntentSenderParams
import java.net.URL
import javax.inject.Inject

/**
 * Базовая VM, служит как прокси для старта [DownloadService] и наблюдения за результатом в UI в
 * случае необходимости.
 *
 * Запоминает текущие запросы загрузки (см. [enqueueDownload]) и позволяет получить по ним статус или
 * ошибку, которую может устранить юзер (см. [recoverableExceptions]), предоставив разрешение на доступ.
 */
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepo: DownloadsRepo,
    private val downloadManager: DownloadManager,
    @Dispatcher(AppDispatchers.IO)
    private val ioDispatcher: CoroutineDispatcher,
    @BaseJson
    private val json: Json,
    state: SavedStateHandle,
) : BaseHandleableViewModel(state) {

    val downloadsInfos: LiveData<List<DownloadInfo>> = downloadRepo.get().asLiveData()

    /**
     * Эмитит [IntentSenderParams], содержащие [android.content.IntentSender]
     * в случае возникновения ошибки доступа при записи/чтении "чужих" файлов в MediaStore. В этом
     * случае у пользователя надо запросить доступ к таким файлам через intent.
     */
    val recoverableExceptions: LiveData<VmEvent<IntentSenderParams>?> = downloadsInfos.switchMap { list ->
        downloadRepo.getIntentSenderListFiltered(list.map { it.name }).asLiveData()
    }

    private val failedStartParamsEvent = MutableStateFlow<VmEvent<DownloadService.Params>?>(null)

    override fun onInitialized() {
        super.onInitialized()
        viewModelScope.launch {
            downloadManager.successAddedToQueueEvents.collect {
                it.targetResourceName.takeIf { it.isNotEmpty() }?.let { name ->
                    showSnackbar(
                        SnackbarAction(
                            TextMessage(
                                R.string.download_toast_success_add_to_queue_message_format,
                                name
                            )
                        )
                    )
                }
            }
        }
        viewModelScope.launch {
            downloadManager.failedAddedToQueueEvents.collect {
                val name = it.first.targetResourceName
                val reason = TextMessage.ResArg(
                    when (it.second) {
                        FailAddReason.NOT_VALID -> R.string.download_failed_add_to_queue_reason_not_valid
                        FailAddReason.ALREADY_ADDED -> R.string.download_failed_add_to_queue_reason_already_added
                        FailAddReason.ALREADY_LOADING -> R.string.download_failed_add_to_queue_reason_already_loading
                    }
                )
                val message: TextMessage? = if (name.isNotEmpty()) {
                    TextMessage(
                        R.string.download_alert_failed_add_to_queue_name_message_format,
                        name,
                        reason
                    )
                } else {
                    val url = it.first.url
                    if (url.isNotEmpty()) {
                        TextMessage(
                            R.string.download_alert_failed_add_to_queue_url_message_format,
                            name,
                            reason
                        )
                    } else {
                        null
                    }
                }
                message?.let {
                    showOkDialog(DIALOG_TAG_FAILED_ADD_TO_QUEUE, message)
                }
            }
        }
        viewModelScope.launch {
            downloadManager.failedStartParamsEvents.collect {
                showOkDialog(
                    DIALOG_TAG_FAILED_START,
                    TextMessage(
                        R.string.download_alert_failed_start_message_format,
                        it.targetResourceName
                    ),
                    TextMessage(R.string.download_alert_failed_start_title)
                )
            }
        }
    }

    override fun handleAlerts(context: Context, delegate: AlertFragmentDelegate<*>) {
        super.handleAlerts(context, delegate)
        delegate.bindAlertDialog(DIALOG_TAG_FAILED_ADD_TO_QUEUE) {
            it.asOkDialog(context)
        }
        delegate.bindAlertDialog(DIALOG_TAG_FAILED_START) {
            it.asOkDialog(context)
        }
    }

    fun downloadFromJson(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(ioDispatcher) {
            json.decodeFromStringOrNull<List<DownloadParamsModel>>(uri.readString(contentResolver)).orEmpty()
                .let { list ->
                    if (list.isNotEmpty()) {
                        list.forEach {
                            it.bodyUri?.toUri()?.takePersistableReadPermission(contentResolver)
                            // тип заранее неизвестен, не игнорируем из ответа
                            downloadManager.enqueueDownloadSuspended(it.toParams())
                            // delay необходим из-за особенности кривых suspend'ов:
                            // следом за незавершённым enqueueDownloadSuspended пойдёт ещё один
                            delay(500)
                        }
                    } else {
                        showSnackbar(SnackbarAction(TextMessage(R.string.download_snackbar_no_valid_params)))
                    }
                }
        }
    }

    /**
     * @param mimeType заранее известный тип из ответа, если есть
     */
    @JvmOverloads
    fun enqueueDownload(paramsModel: DownloadParamsModel, mimeType: String? = null) {
        enqueueDownload(paramsModel.toParams(mimeType))
    }

    /**
     * Стартует загрузку ресурса с параметрами [params]
     */
    fun enqueueDownload(params: DownloadService.Params) {
        downloadManager.enqueueDownload(params)
    }

    @JvmOverloads
    fun observeDownloadPOST(
        uri: URL?,
        @DrawableRes smallIconResId: Int,
        resource: LoadState<*>?,
        body: DownloadService.RequestParams.Body,
        fileName: String? = null,
        format: FileFormat? = null,
        notification: DownloadService.NotificationParams = DownloadService.NotificationParams(
            smallIconResId,
            successActions = defaultNotificationActions(
                baseApplicationContext
            ),
        ),
    ): LiveData<LoadState<DownloadInfoWithUri>> {
        resource ?: return MutableLiveData(
            LoadState.error(
                TextMessageException(
                    TextMessage(messageResId = R.string.download_error)
                )
            )
        )
        uri ?: return MutableLiveData(resource.copyOf())
        return observeDownload(
            defaultPOSTServiceParamsFor(
                uri.toString(),
                fileName,
                body,
                format = format,
                notificationParams = notification
            )
        )
    }

    @JvmOverloads
    fun observeDownloadGET(
        uri: URL?,
        @DrawableRes smallIconResId: Int,
        resource: LoadState<*>?,
        fileName: String? = null,
        format: FileFormat? = null,
        notification: DownloadService.NotificationParams = DownloadService.NotificationParams(
            smallIconResId,
            successActions = defaultNotificationActions(baseApplicationContext)
        ),
    ): LiveData<LoadState<DownloadInfoWithUri>> {
        resource ?: return MutableLiveData(
            LoadState.error(
                TextMessageException(
                    TextMessage(messageResId = R.string.download_error)
                )
            )
        )
        uri ?: return MutableLiveData(resource.copyOf())
        return observeDownload(
            defaultGETServiceParamsFor(
                uri.toString(),
                fileName,
                format = format,
                notificationParams = notification
            )
        )
    }

    // FIXME исправить логику на notifier
    private fun observeDownload(params: DownloadService.Params): LiveData<LoadState<DownloadInfoWithUri>> {
        // на этом моменте нет гарантий, что resourceMimeType от клиентского кода совпадёт с тем,
        // что будет в сервисе после получения респонса ->
        // возможен поиск только по имени без расширения
        val resourceName = params.resourceNameWithoutExt
        return downloadsInfos
            .mapNotNull { info ->
                info.find { it.name == resourceName }?.let {
                    DownloadInfoWithUri(Uri.parse(params.requestParams.url), it)
                }
            }
            .map {
                when (it.downloadInfo.status) {
                    is DownloadInfo.Status.Loading -> LoadState.loading(it)
                    is DownloadInfo.Status.Error -> LoadState.error(
                        it.downloadInfo.statusAsError?.reason ?: RuntimeException(),
                        it
                    )

                    is DownloadInfo.Status.Success -> LoadState.success<DownloadInfoWithUri>(it)
                }
            }
            .unsubscribeIf { !it.isLoading }
    }

    fun observeOnce(
        owner: LifecycleOwner,
        infoFunc: (DownloadInfo) -> Boolean,
        statusChangeCallback: (DownloadInfo) -> Unit,
    ) {
        val observer = object : Observer<List<DownloadInfo>?> {
            override fun onChanged(value: List<DownloadInfo>?) {
                value?.find(infoFunc)?.let { info ->
                    if (info.resourceStatus == Status.SUCCESS
                            || info.resourceStatus == Status.ERROR
                    ) {
                        downloadsInfos.removeObserver(this)
                    }
                    statusChangeCallback(info)
                }
            }
        }
        downloadsInfos.observe(owner, observer)
    }

    data class DownloadInfoWithUri(
        val uri: Uri,
        val downloadInfo: DownloadInfo,
    )

    companion object {

        const val DIALOG_TAG_FAILED_ADD_TO_QUEUE = "failed_add_to_queue"
        const val DIALOG_TAG_FAILED_START = "failed_start"

        @JvmStatic
        @JvmOverloads
        fun defaultNotificationActions(
            context: Context,
            @StringRes shareChooseClientTitle: Int = net.maxsmr.core.ui.R.string.chooser_title_send,
            @StringRes viewChooseClientTitle: Int = net.maxsmr.core.ui.R.string.chooser_title_view,
            @DrawableRes shareIconResId: Int = android.R.drawable.ic_menu_share,
            @DrawableRes viewIconResId: Int = android.R.drawable.ic_menu_view,
            subject: String = EMPTY_STRING,
            text: String = EMPTY_STRING,
            emails: ArrayList<String> = arrayListOf(),
        ): MutableSet<DownloadService.NotificationParams.SuccessAction> = mutableSetOf(
            DownloadService.NotificationParams.SuccessAction.Share(
                context.getString(shareChooseClientTitle),
                context.getString(R.string.download_notification_success_share_button),
                shareIconResId,
                subject,
                text,
                emails
            ),
            DownloadService.NotificationParams.SuccessAction.View(
                context.getString(viewChooseClientTitle),
                context.getString(R.string.download_notification_success_view_button),
                viewIconResId
            ),
        )

        @JvmStatic
        private fun DownloadParamsModel.toParams(mimeType: String? = null): DownloadService.Params = with(this) {
            val url = url.trim()
            val bodyUri = bodyUri
            val targetHashInfo = targetSha1Hash?.takeIf { it.isNotEmpty() }?.let {
                HashInfo(ALGORITHM_SHA1, it)
            } ?: HashInfo(ALGORITHM_SHA1, EMPTY_STRING) // если не указан - считаем в итоге по тому же алгоритму

            val notificationParams = DownloadService.NotificationParams(
                successActions = defaultNotificationActions(baseApplicationContext)
            )

            // не спрашивать из ответа тип, если он известен заранее
            val hasMimeType = !mimeType.isNullOrEmpty()

            if (method == Method.POST && bodyUri != null) {
                defaultPOSTServiceParamsFor(
                    url,
                    fileName,
                    DownloadService.RequestParams.Body(
                        DownloadService.RequestParams.Body.Uri(bodyUri.trim())
                    ),
                    ignoreContentType = hasMimeType,
                    ignoreAttachment = ignoreAttachment,
                    ignoreFileName = ignoreFileName,
                    storeErrorBody = ignoreServerErrors,
                    headers = headers,
                    subDir = subDirName,
                    targetHashInfo = targetHashInfo,
                    replaceFile = replaceFile,
                    deleteUnfinished = deleteUnfinished,
                    notificationParams = notificationParams,
                )
            } else {
                defaultGETServiceParamsFor(
                    url,
                    fileName,
                    ignoreContentType = hasMimeType,
                    ignoreAttachment = ignoreAttachment,
                    ignoreFileName = ignoreFileName,
                    storeErrorBody = ignoreServerErrors,
                    headers = headers,
                    subDir = subDirName,
                    targetHashInfo = targetHashInfo,
                    replaceFile = replaceFile,
                    deleteUnfinished = deleteUnfinished,
                    notificationParams = notificationParams,
                )
            }
        }.apply {
            if (!mimeType.isNullOrEmpty()) {
                resourceMimeType = mimeType
            }
        }
    }
}