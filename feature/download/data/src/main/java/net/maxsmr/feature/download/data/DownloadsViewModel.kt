package net.maxsmr.feature.download.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.maxsmr.commonutils.ALGORITHM_SHA1
import net.maxsmr.commonutils.flow.takeWhileInclusive
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.commonutils.media.readString
import net.maxsmr.commonutils.media.takePersistableReadPermission
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.database.model.download.DownloadInfo
import net.maxsmr.core.di.BaseJson
import net.maxsmr.core.domain.entities.feature.download.DownloadParamsModel
import net.maxsmr.core.domain.entities.feature.download.HashInfo
import net.maxsmr.core.domain.entities.feature.network.Method
import net.maxsmr.core.utils.kotlinx.serialization.decodeFromStringOrNull
import net.maxsmr.feature.download.data.DownloadService.Params.Companion.defaultGETServiceParamsFor
import net.maxsmr.feature.download.data.DownloadService.Params.Companion.defaultPOSTServiceParamsFor
import net.maxsmr.feature.download.data.DownloadService.RequestParams.MimeTypeMatchRule
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
    @ApplicationContext private val context: Context,
    state: SavedStateHandle,
) : BaseViewModel(state, context) {

    val downloadsInfos: Flow<List<DownloadInfo>> = downloadRepo.get()

    /**
     * Эмитит [IntentSenderParams], содержащие [android.content.IntentSender]
     * в случае возникновения ошибки доступа при записи/чтении "чужих" файлов в MediaStore. В этом
     * случае у пользователя надо запросить доступ к таким файлам через intent.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val recoverableExceptions: Flow<VmEvent<IntentSenderParams>?> = downloadsInfos.flatMapLatest { list ->
        downloadRepo.getIntentSenderParamsFiltered(list.map { it.name }).map { VmEvent(it) }
    }

    override fun onInitialized() {
        downloadManager.successAddedToQueueEvent.observe {
            it.targetResourceName.takeIf { res -> res.isNotEmpty() }?.let { name ->
                showSnackbar(
                    TextMessage(
                        R.string.download_toast_success_add_to_queue_message_format,
                        name
                    )
                )
            }
        }

        downloadManager.failedAddedToQueueEvent.observe {
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


        downloadManager.failedStartParamsEvent.observe {
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

    fun downloadFromJson(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch(Dispatchers.IO) {
            json.decodeFromStringOrNull<List<DownloadParamsModel>>(uri.readString(contentResolver)).orEmpty()
                .let { list ->
                    if (list.isNotEmpty()) {
                        list.forEach {
                            it.bodyUri?.toUri()?.takePersistableReadPermission(contentResolver)
                            // тип заранее неизвестен, не игнорируем из ответа
                            downloadManager.enqueueDownloadSuspended(it.toParams(context))
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
        val params = paramsModel.toParams(context, mimeType, mimeTypeRule)
        enqueueDownload(params)
        return params
    }

    /**
     * Стартует загрузку ресурса с параметрами [params]
     */
    fun enqueueDownload(params: DownloadService.Params) {
        downloadManager.enqueueDownload(params)
    }

    fun takeOnceDownloadByParams(
        params: DownloadService.Params,
        removeWhenFinished: Boolean = true,
        isSameFunc: (DownloadService.Params.(DownloadService.Params) -> Boolean)? = null,
    ) = takeDownloadByParamsInternal(params, removeWhenFinished, isSameFunc).takeWhileInclusive { it.isLoading }

    fun takeDownloadByParams(
        params: DownloadService.Params,
        isSameFunc: (DownloadService.Params.(DownloadService.Params) -> Boolean)? = null,
    ): Flow<LoadState<DownloadInfoWithParams>> {
        return takeDownloadByParamsInternal(params, false, isSameFunc)
    }

    fun <P> takeOnceDownload(
        params: P,
        removeWhenFinished: Boolean = true,
        isSameFunc: (DownloadService.Params.(P) -> Boolean),
    ) = takeDownloadInternal(params, removeWhenFinished, isSameFunc).takeWhileInclusive { it.isLoading }

    fun <P> takeDownload(
        params: P,
        isSameFunc: (DownloadService.Params.(P) -> Boolean),
    ): Flow<LoadState<DownloadInfoWithParams>> {
        return takeDownloadInternal(params, false, isSameFunc)
    }

    fun takeOnceDownloadByInfo(
        infoPredicate: (DownloadInfo) -> Boolean,
        statusChangeCallback: (DownloadInfo) -> Unit,
    ): Job {
        return downloadsInfos.takeWhileInclusive { list ->
            return@takeWhileInclusive list.find(infoPredicate)?.let { info ->
                statusChangeCallback(info)
                // перестать собирать, если стал !isLoading
                info.isLoading
            } ?: false
        }.launchIn(viewModelScope)
    }

    private fun takeDownloadByParamsInternal(
        params: DownloadService.Params,
        removeWhenFinished: Boolean,
        isSameFunc: (DownloadService.Params.(DownloadService.Params) -> Boolean)? = null,
    ): Flow<LoadState<DownloadInfoWithParams>> {
        return (if (isSameFunc == null) {
            downloadManager.takeDownloadByParams(params, removeWhenFinished)
        } else {
            downloadManager.takeDownloadByParams(params, removeWhenFinished, isSameFunc)
        })
    }

    private fun <P> takeDownloadInternal(
        params: P,
        removeWhenFinished: Boolean,
        isSameFunc: (DownloadService.Params.(P) -> Boolean),
    ): Flow<LoadState<DownloadInfoWithParams>> {
        return downloadManager.takeDownload(params, removeWhenFinished, isSameFunc)
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
            @DrawableRes shareIconResId: Int = android.R.drawable.ic_menu_share,
            @DrawableRes viewIconResId: Int = android.R.drawable.ic_menu_view,
            subject: String = EMPTY_STRING,
            text: String = EMPTY_STRING,
            emails: ArrayList<String> = arrayListOf(),
        ): MutableSet<DownloadService.NotificationParams.SuccessAction> = mutableSetOf(
            DownloadService.NotificationParams.SuccessAction.Share(
                context.getString(R.string.download_notification_success_share_button),
                shareIconResId,
                subject,
                text,
                emails
            ),
            DownloadService.NotificationParams.SuccessAction.View(
                context.getString(R.string.download_notification_success_view_button),
                viewIconResId
            ),
        )

        @JvmStatic
        fun DownloadParamsModel.toParams(
            context: Context,
            mimeType: String? = null,
            mimeTypeRule: MimeTypeMatchRule? = MimeTypeMatchRule.None,
        ): DownloadService.Params = with(this) {
            val url = url.trim()
            val bodyUri = bodyUri?.trim()
            val targetHashInfo = targetSha1Hash?.takeIf { it.isNotEmpty() }?.let {
                HashInfo(ALGORITHM_SHA1, it)
            } ?: HashInfo(ALGORITHM_SHA1, EMPTY_STRING) // если не указан - считаем в итоге по тому же алгоритму

            val notificationParams = DownloadService.NotificationParams(
                successActions = defaultSuccessNotificationActions(context)
            )

            // не спрашивать из ответа тип, если он известен заранее
            val hasMimeType = !mimeType.isNullOrEmpty()

            if (method == Method.POST && !bodyUri.isNullOrEmpty()) {
                defaultPOSTServiceParamsFor(
                    url,
                    fileName,
                    DownloadService.RequestParams.Body(
                        context,
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