package net.maxsmr.feature.download.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.maxsmr.commonutils.ALGORITHM_SHA1
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.commonutils.live.unsubscribeIf
import net.maxsmr.commonutils.media.readString
import net.maxsmr.commonutils.media.takePersistableReadPermission
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.database.model.download.DownloadInfo
import net.maxsmr.core.di.BaseJson
import net.maxsmr.core.domain.entities.feature.download.DownloadParamsModel
import net.maxsmr.core.domain.entities.feature.download.HashInfo
import net.maxsmr.core.domain.entities.feature.network.Method
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.asOkDialog
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.core.utils.decodeFromStringOrNull
import net.maxsmr.feature.download.data.DownloadService.Params.Companion.defaultGETServiceParamsFor
import net.maxsmr.feature.download.data.DownloadService.Params.Companion.defaultPOSTServiceParamsFor
import net.maxsmr.feature.download.data.DownloadService.RequestParams.MimeTypeMatchRule
import net.maxsmr.feature.download.data.manager.DownloadInfoResultData
import net.maxsmr.feature.download.data.manager.DownloadManager
import net.maxsmr.feature.download.data.manager.DownloadManager.FailAddReason
import net.maxsmr.feature.download.data.model.IntentSenderParams
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
    @BaseJson private val json: Json,
    state: SavedStateHandle,
) : BaseHandleableViewModel(state) {

    val downloadsInfos: LiveData<List<DownloadInfo>> = downloadRepo.get().asLiveData()

    val downloadItems: LiveData<List<DownloadInfoResultData>> = downloadManager.resultItems.asLiveData()

    /**
     * Эмитит [IntentSenderParams], содержащие [android.content.IntentSender]
     * в случае возникновения ошибки доступа при записи/чтении "чужих" файлов в MediaStore. В этом
     * случае у пользователя надо запросить доступ к таким файлам через intent.
     */
    val recoverableExceptions: LiveData<VmEvent<IntentSenderParams>?> = downloadsInfos.switchMap { list ->
        downloadRepo.getIntentSenderListFiltered(list.map { it.name }).asLiveData()
    }

    val failedStartParams = downloadManager.failedStartParamsFlow.asLiveData()

    override fun onInitialized() {
        super.onInitialized()

        viewModelScope.launch {
            downloadManager.successAddedToQueueEvents.collect {
                it.targetResourceName.takeIf { res -> res.isNotEmpty() }?.let { name ->
                    showSnackbar(
                        TextMessage(
                            R.string.download_toast_success_add_to_queue_message_format,
                            name
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
                        R.string.download_dialog_failed_add_to_queue_name_message_format,
                        name,
                        reason
                    )
                } else {
                    val url = it.first.url
                    if (url.isNotEmpty()) {
                        TextMessage(
                            R.string.download_dialog_failed_add_to_queue_url_message_format,
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
                        R.string.download_dialog_failed_start_message_format,
                        it.targetResourceName
                    ),
                    TextMessage(R.string.download_dialog_failed_start_title)
                )
            }
        }

    }

    override fun handleAlerts(delegate: AlertFragmentDelegate<*>) {
        super.handleAlerts(delegate)
        val context = delegate.context
        with(delegate) {
            bindAlertDialog(DIALOG_TAG_FAILED_ADD_TO_QUEUE) {
                it.asOkDialog(context)
            }
            bindAlertDialog(DIALOG_TAG_FAILED_START) {
                it.asOkDialog(context)
            }
        }
    }

    fun downloadFromJson(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
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
                        showSnackbar(TextMessage(R.string.download_snackbar_no_valid_params))
                    }
                }
        }
    }

    /**
     * @param mimeType заранее известный тип из ответа, если есть
     */
    @JvmOverloads
    fun enqueueDownload(
        paramsModel: DownloadParamsModel,
        mimeType: String? = null,
        mimeTypeRule: MimeTypeMatchRule? = MimeTypeMatchRule.None,
    ): DownloadService.Params {
        val params = paramsModel.toParams(mimeType, mimeTypeRule)
        enqueueDownload(params)
        return params
    }

    /**
     * Стартует загрузку ресурса с параметрами [params]
     */
    fun enqueueDownload(params: DownloadService.Params) {
        downloadManager.enqueueDownload(params)
    }

    fun observeOnceDownloadByParams(
        params: DownloadService.Params,
        removeWhenFinished: Boolean = true,
        isSameFunc: (DownloadService.Params.(DownloadService.Params) -> Boolean)? = null,
    ) = observeDownloadByParamsInternal(params, removeWhenFinished, isSameFunc).unsubscribeIf { !it.isLoading }

    fun observeDownloadByParams(
        params: DownloadService.Params,
        isSameFunc: (DownloadService.Params.(DownloadService.Params) -> Boolean)? = null,
    ): LiveData<LoadState<DownloadInfoWithParams>> {
        return observeDownloadByParamsInternal(params, false, isSameFunc)
    }

    private fun observeDownloadByParamsInternal(
        params: DownloadService.Params,
        removeWhenFinished: Boolean,
        isSameFunc: (DownloadService.Params.(DownloadService.Params) -> Boolean)? = null,
    ): LiveData<LoadState<DownloadInfoWithParams>> {
        return (if (isSameFunc == null) {
            downloadManager.observeDownloadByParams(params, removeWhenFinished)
        } else {
            downloadManager.observeDownloadByParams(params, removeWhenFinished, isSameFunc)
        }).asLiveData().distinctUntilChanged()
    }

    fun <P> observeOnceDownload(
        params: P,
        removeWhenFinished: Boolean = true,
        isSameFunc: (DownloadService.Params.(P) -> Boolean),
    ) = observeDownloadInternal(params, removeWhenFinished, isSameFunc).unsubscribeIf { !it.isLoading }

    fun <P> observeDownload(
        params: P,
        isSameFunc: (DownloadService.Params.(P) -> Boolean),
    ): LiveData<LoadState<DownloadInfoWithParams>> {
        return observeDownloadInternal(params, false, isSameFunc)
    }

    private fun <P> observeDownloadInternal(
        params: P,
        removeWhenFinished: Boolean,
        isSameFunc: (DownloadService.Params.(P) -> Boolean),
    ): LiveData<LoadState<DownloadInfoWithParams>> {
        return downloadManager.observeDownload(params, removeWhenFinished, isSameFunc).asLiveData()
            .distinctUntilChanged()
    }

    fun observeOnceDownloadByInfo(
        owner: LifecycleOwner,
        infoFunc: (DownloadInfo) -> Boolean,
        statusChangeCallback: (DownloadInfo) -> Unit,
    ) {
        val observer = object : Observer<List<DownloadInfo>?> {
            override fun onChanged(value: List<DownloadInfo>?) {
                value?.find(infoFunc)?.let { info ->
                    if (!info.isLoading) {
                        downloadsInfos.removeObserver(this)
                    }
                    statusChangeCallback(info)
                }
            }
        }
        downloadsInfos.observe(owner, observer)
    }

    /**
     * @param downloadInfo отсутствует, если старт / добавление в очередь не удались
     */
    data class DownloadInfoWithParams(
        val params: DownloadService.Params,
        val downloadInfo: DownloadInfo?,
    )

    companion object {

        const val DIALOG_TAG_FAILED_ADD_TO_QUEUE = "failed_add_to_queue"
        const val DIALOG_TAG_FAILED_START = "failed_start"

        @JvmStatic
        @JvmOverloads
        fun defaultSuccessNotificationActions(
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
        fun DownloadParamsModel.toParams(
            mimeType: String? = null,
            mimeTypeRule: MimeTypeMatchRule? = MimeTypeMatchRule.None,
        ): DownloadService.Params = with(this) {
            val url = url.trim()
            val bodyUri = bodyUri?.trim()
            val targetHashInfo = targetSha1Hash?.takeIf { it.isNotEmpty() }?.let {
                HashInfo(ALGORITHM_SHA1, it)
            } ?: HashInfo(ALGORITHM_SHA1, EMPTY_STRING) // если не указан - считаем в итоге по тому же алгоритму

            val notificationParams = DownloadService.NotificationParams(
                successActions = defaultSuccessNotificationActions(baseApplicationContext)
            )

            // не спрашивать из ответа тип, если он известен заранее
            val hasMimeType = !mimeType.isNullOrEmpty()

            if (method == Method.POST && !bodyUri.isNullOrEmpty()) {
                defaultPOSTServiceParamsFor(
                    url,
                    fileName,
                    DownloadService.RequestParams.Body(
                        DownloadService.RequestParams.Body.Uri(bodyUri),
                    ),
                    ignoreAttachment = ignoreAttachment,
                    ignoreFileName = ignoreFileName,
                    storeErrorBody = ignoreServerErrors,
                    contentTypeRule = if (hasMimeType) null else mimeTypeRule,
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
                    ignoreAttachment = ignoreAttachment,
                    ignoreFileName = ignoreFileName,
                    storeErrorBody = ignoreServerErrors,
                    contentTypeRule = if (hasMimeType) null else mimeTypeRule,
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