package net.maxsmr.feature.download.ui

import android.content.DialogInterface
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.commonutils.media.isEmpty
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.content.IntentWithUriProvideStrategy
import net.maxsmr.core.android.content.ShareStrategy
import net.maxsmr.core.android.content.ViewStrategy
import net.maxsmr.feature.download.data.DownloadService
import net.maxsmr.feature.download.data.DownloadStateNotifier
import net.maxsmr.feature.download.data.manager.DownloadInfoResultData
import net.maxsmr.feature.download.data.manager.DownloadManager
import net.maxsmr.feature.download.ui.adapter.DownloadInfoAdapterData
import javax.inject.Inject

@HiltViewModel
class DownloadsStateViewModel @Inject constructor(
    state: SavedStateHandle,
    private val manager: DownloadManager,
) : BaseViewModel(state) {

    val queueNames = MutableStateFlow<List<String>>(emptyList())

    val allItems = MutableStateFlow<List<DownloadInfoAdapterData>>(emptyList())

    val currentItems = MutableStateFlow<List<DownloadInfoAdapterData>>(emptyList())

    val anyCanBeCancelled = currentItems
        .map { it.any { item -> item.downloadInfo.isLoading } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val queryNameFilter = MutableLiveData<String>()

    private val _navigateUriEvent = MutableStateFlow<VmEvent<IntentWithUriProvideStrategy<*>>?>(null)
    val navigateUriEvent = _navigateUriEvent as StateFlow<VmEvent<IntentWithUriProvideStrategy<*>>?>

    override fun onInitialized() {
        super.onInitialized()

        manager.downloadsPendingParams.observe {
            queueNames.value = it.map { params -> params.targetResourceName }
        }
        manager.resultItems.observe {
            allItems.value = it.map { item -> DownloadInfoAdapterData(item) }
            currentItems.value = it.mapWithFilterByName(queryNameFilter.value.orEmpty())
        }

        queueNames.observe {
            if (it.isEmpty()) {
                dialogQueue.removeAllWithTag(DIALOG_TAG_CLEAR_QUEUE)
            }
        }
        anyCanBeCancelled.observe {
            if (!it) {
                dialogQueue.removeAllWithTag(DIALOG_TAG_CANCEL_ALL)
            }
        }
        currentItems.observe { list ->
            if (!list.any { it.state is DownloadStateNotifier.DownloadState.Success }) {
                dialogQueue.removeAllWithTag(DIALOG_TAG_RETRY_IF_SUCCESS)
            }
        }
        queryNameFilter.observe {
            currentItems.value = manager.resultItems.value.mapWithFilterByName(it)
        }
    }

    fun onClearQueue() {
        if (queueNames.value.isNotEmpty()) {
            showYesNoDialog(
                DIALOG_TAG_CLEAR_QUEUE,
                TextMessage(R.string.download_dialog_clear_queue_message),
                TextMessage(R.string.download_dialog_confirm_title),
                onSelect = {
                    if (it == DialogInterface.BUTTON_POSITIVE) {
                        manager.cancelAllPending()
                    }
                }
            )
        }
    }

    fun onCancelAllDownloads() {
        if (anyCanBeCancelled.value) {
            showYesNoDialog(
                DIALOG_TAG_CANCEL_ALL,
                TextMessage(R.string.download_dialog_cancel_all_message),
                TextMessage(R.string.download_dialog_confirm_title),
                onSelect = {
                    if (it == DialogInterface.BUTTON_POSITIVE) {
                        DownloadService.cancelAll()
                    }
                }
            )
        }
    }

    fun onRetryDownload(
        downloadId: Long,
        params: DownloadService.Params,
        state: DownloadStateNotifier.DownloadState?,
    ) {
        if (state is DownloadStateNotifier.DownloadState.Success) {
            showYesNoDialog(
                DIALOG_TAG_RETRY_IF_SUCCESS,
                TextMessage(R.string.download_dialog_retry_if_success_message),
                TextMessage(R.string.download_dialog_confirm_title),
                onSelect = {
                    if (it == DialogInterface.BUTTON_POSITIVE) {
                        manager.retryDownloadWithParams(downloadId, params)
                    }
                }
            )
        } else {
            manager.retryDownloadWithParams(downloadId, params)
        }
    }

    fun onClearFinished() {
        manager.removeAllFinished()
    }

    fun onCancelDownload(id: Long) {
        manager.cancelDownload(id)
    }

    fun onRemoveFinishedDownload(id: Long) {
        manager.removeFinished(id)
    }

    fun onNameQueryFilterChanged(value: String?) {
        queryNameFilter.value = value.orEmpty()/*.trim()*/
    }

    fun onDeleteResource(downloadId: Long, name: String) {
        showYesNoDialog(
            DIALOG_TAG_DELETE_IF_SUCCESS,
            TextMessage(R.string.download_dialog_delete_if_success_message_format, name),
            TextMessage(R.string.download_dialog_confirm_title),
            onSelect = {
                if (it == DialogInterface.BUTTON_POSITIVE) {
                    manager.removeFinished(downloadId, withDb = true, withUri = true)
                }
            }
        )
    }

    fun onViewResource(downloadUri: Uri, mimeType: String) {
        navigateUriAfterCheck(
            downloadUri,
            ViewStrategy(IntentWithUriProvideStrategy.Data(downloadUri, mimeType))
        )
    }

    fun onShareResource(downloadUri: Uri, mimeType: String) {
        navigateUriAfterCheck(
            downloadUri,
            ShareStrategy(ShareStrategy.Data(downloadUri, mimeType))
        )
    }

    private fun <T : IntentWithUriProvideStrategy<*>> navigateUriAfterCheck(downloadUri: Uri, strategy: T) {
        viewModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main.immediate) {
                AlertDialogBuilder(DIALOG_TAG_PROGRESS).build()
            }
            if (downloadUri.isEmpty(baseApplicationContext.contentResolver)) {
                showSnackbar(
                    TextMessage(
                        R.string.download_snackbar_action_view_error_format,
                        downloadUri.toString()
                    )
                )
            } else {
                _navigateUriEvent.emit(VmEvent(strategy))
            }
            withContext(Dispatchers.Main.immediate) {
                dialogQueue.removeAllWithTag(DIALOG_TAG_PROGRESS)
            }
        }
    }

    private fun List<DownloadInfoResultData>?.mapWithFilterByName(query: String): List<DownloadInfoAdapterData> {
        val items = this?.map { item -> DownloadInfoAdapterData(item) }.orEmpty()
        return if (query.isNotEmpty()) {
            items.filter {
                it.downloadInfo.nameWithExt.contains(query, ignoreCase = true)
            }
        } else {
            items
        }
    }

    companion object {

        const val DIALOG_TAG_CLEAR_QUEUE = "clear_queue"
        const val DIALOG_TAG_CANCEL_ALL = "cancel_all"
        const val DIALOG_TAG_RETRY_IF_SUCCESS = "retry_if_success"
        const val DIALOG_TAG_DELETE_IF_SUCCESS = "delete_if_success"
    }
}