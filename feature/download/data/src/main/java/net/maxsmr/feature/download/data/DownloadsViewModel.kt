package net.maxsmr.feature.download.data

import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.gui.message.TextMessageException
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.commonutils.live.mapNotNull
import net.maxsmr.commonutils.live.unsubscribeIf
import net.maxsmr.commonutils.states.ILoadState.Companion.copyOf
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.commonutils.states.Status
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.actions.ToastAction
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.content.FileFormat
import net.maxsmr.core.database.model.download.DownloadInfo
import net.maxsmr.core.di.AppDispatchers
import net.maxsmr.core.di.Dispatcher
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment.Companion.handleNavigation
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
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
 * Запоминает текущие запросы загрузки (см. [download]) и позволяет получить по ним статус или
 * ошибку, которую может устранить юзер (см. [recoverableExceptions]), предоставив разрешение на доступ.
 */
@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadRepo: DownloadsRepo,
    private val downloadManager: DownloadManager,
    @Dispatcher(AppDispatchers.IO)
    private val ioDispatcher: CoroutineDispatcher,
    state: SavedStateHandle,
) : BaseViewModel(state) {

    val downloadsInfos: LiveData<List<DownloadInfo>> = downloadRepo.get().asLiveData()

    /**
     * Эмитит [IntentSenderParams], содержащие [android.content.IntentSender]
     * в случае возникновения ошибки доступа при записи/чтении "чужих" файлов в MediaStore. В этом
     * случае у пользователя надо запросить доступ к таким файлам через intent.
     */
    val recoverableExceptions: LiveData<VmEvent<IntentSenderParams>?> = downloadsInfos.switchMap { list ->
        downloadRepo.getIntentSenderListFiltered(list.map { it.name }).asLiveData()
    }

    private val failedStartParamsEvent = MutableLiveData<VmEvent<DownloadService.Params>>()

    override fun onInitialized() {
        super.onInitialized()
        viewModelScope.launch {
            downloadManager.failedStartParamsEvent.collect {
                failedStartParamsEvent.postValue(VmEvent(it))
            }
        }
        viewModelScope.launch {
            downloadManager.successAddedToQueueEvent.collect {
                it.targetResourceName.takeIf { it.isNotEmpty() }?.let { name ->
                    showToast(
                        ToastAction(
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
            downloadManager.failedAddedToQueueEvent.collect {
                val name = it.first.targetResourceName
                val reason = TextMessage.ResArg(
                    when (it.second) {
                        FailAddReason.NOT_VALID -> R.string.download_fail_add_to_queue_reason_not_valid
                        FailAddReason.ALREADY_ADDED -> R.string.download_fail_add_to_queue_reason_already_added
                        FailAddReason.ALREADY_LOADING -> R.string.download_fail_add_to_queue_reason_already_loading
                    }
                )
                val message: TextMessage? = if (name.isNotEmpty()) {
                    TextMessage(
                        R.string.download_toast_fail_add_to_queue_name_message_format,
                        name,
                        reason
                    )
                } else {
                    val url = it.first.url
                    if (url.isNotEmpty()) {
                        TextMessage(
                            R.string.download_toast_fail_add_to_queue_url_message_format,
                            name,
                            reason
                        )
                    } else {
                        null
                    }
                }
                showToast(ToastAction(message, duration = ToastAction.ToastLength.LONG))
            }
        }
    }

    // вызвать на используемом фрагменте
    fun handleEvents(fragment: BaseVmFragment<*>) {
        if (fragment is BaseNavigationFragment<*, *>) {
            navigationCommands.observeEvents {
                fragment.handleNavigation(it)
            }
        }
        toastCommands.observeEvents(fragment.viewLifecycleOwner) { it.doAction(fragment.requireContext()) }

        failedStartParamsEvent.observeEvents {
            showToast(
                ToastAction(
                    TextMessage(
                        R.string.download_toast_start_failed_message_format,
                        it.targetResourceName
                    )
                )
            )
        }
    }

    /**
     * Стартует загрузку ресурса с параметрами [params]
     */
    fun download(params: DownloadService.Params) {
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
                uri,
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
            DownloadService.Params.defaultGETServiceParamsFor(
                uri,
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
                    is DownloadInfo.Status.Loading -> LoadState.loading()
                    is DownloadInfo.Status.Error -> LoadState.error(
                        it.downloadInfo.statusAsError?.reason ?: RuntimeException()
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

    fun isDownloaded(resourceName: String, initialExt: String): Boolean =
        runBlocking(ioDispatcher) { downloadRepo.isDownloaded(resourceName, initialExt) }

    data class DownloadInfoWithUri(
        val uri: Uri,
        val downloadInfo: DownloadInfo,
    )

    companion object {

        @JvmStatic
        @JvmOverloads
        fun defaultNotificationActions(
            context: Context,
            @StringRes shareChooseClientTitle: Int = R.string.share_choose_client_file_title,
            @StringRes viewChooseClientTitle: Int = R.string.view_choose_client_file_title,
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
    }
}