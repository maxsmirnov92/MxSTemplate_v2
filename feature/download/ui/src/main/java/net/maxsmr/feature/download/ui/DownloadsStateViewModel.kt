package net.maxsmr.feature.download.ui

import android.content.Context
import android.content.DialogInterface
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.commonutils.media.isEmpty
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.content.IntentWithUriProvideStrategy
import net.maxsmr.core.android.content.ShareIntentStrategy
import net.maxsmr.core.android.content.ViewIntentStrategy
import net.maxsmr.feature.download.data.DownloadService
import net.maxsmr.feature.download.data.DownloadStateNotifier
import net.maxsmr.feature.download.data.manager.DownloadInfoResultData
import net.maxsmr.feature.download.data.manager.DownloadManager
import net.maxsmr.feature.download.ui.adapter.DownloadInfoAdapterData
import javax.inject.Inject

@HiltViewModel
class DownloadsStateViewModel @Inject constructor(
    private val manager: DownloadManager,
    @ApplicationContext private val context: Context,
    state: SavedStateHandle,
) : BaseViewModel(state, context) {

    val queueNames: StateFlow<List<String>> by lazy {
        _queueNames.asStateFlow()
    }

    val allItems: StateFlow<List<DownloadInfoAdapterData>> by lazy {
        _allItems.asStateFlow()
    }

    val currentItems: StateFlow<List<DownloadInfoAdapterData>> by lazy {
        _currentItems.asStateFlow()
    }

    val anyCanBeCancelled: StateFlow<Boolean> by lazy {
        _currentItems
            .map { it.any { item -> item.downloadInfo.isLoading } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    }

    val queryNameFilter: StateFlow<String> by lazy {
        _queryNameFilter.asStateFlow()
    }

    val navigateUriEvent: StateFlow<VmEvent<IntentWithUriProvideStrategy<*>>?> by lazy {
        _navigateUriEvent.asStateFlow()
    }

    private val _queueNames = MutableStateFlow<List<String>>(emptyList())

    private val _allItems = MutableStateFlow<List<DownloadInfoAdapterData>>(emptyList())

    private val _currentItems = MutableStateFlow<List<DownloadInfoAdapterData>>(emptyList())

    private val _queryNameFilter = MutableStateFlow(EMPTY_STRING)

    private val _navigateUriEvent = MutableStateFlow<VmEvent<IntentWithUriProvideStrategy<*>>?>(null)

    override fun onInitialized() {
        manager.downloadsPendingParams.observe {
            _queueNames.value = it.map { params -> params.targetResourceName }
        }
        manager.resultItems.observe {
            _allItems.value = it.map { item -> DownloadInfoAdapterData(item) }
            _currentItems.value = it.mapWithFilterByName(queryNameFilter.value)
        }

        _queueNames.observe {
            if (it.isEmpty()) {
                dialogQueue.removeAllWithTag(DIALOG_TAG_CLEAR_QUEUE)
            }
        }
        anyCanBeCancelled.observe {
            if (!it) {
                dialogQueue.removeAllWithTag(DIALOG_TAG_CANCEL_ALL)
            }
        }
        _currentItems.observe { list ->
            if (!list.any { it.state is DownloadStateNotifier.DownloadState.Success }) {
                dialogQueue.removeAllWithTag(DIALOG_TAG_RETRY_IF_SUCCESS)
            }
        }
        queryNameFilter.observe {
            _currentItems.value = manager.resultItems.value.mapWithFilterByName(it)
        }
    }

    fun onClearQueue() {
        if (_queueNames.value.isNotEmpty()) {
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
                        DownloadService.cancelAll(context)
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
        _queryNameFilter.value = value.orEmpty()/*.trim()*/
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
            ViewIntentStrategy(ViewIntentStrategy.ViewData(downloadUri, mimeType))
        )
    }

    fun onShareResource(downloadUri: Uri, mimeType: String) {
        navigateUriAfterCheck(
            downloadUri,
            ShareIntentStrategy(ShareIntentStrategy.ShareData(downloadUri, mimeType))
        )
    }

    private fun <T : IntentWithUriProvideStrategy<*>> navigateUriAfterCheck(downloadUri: Uri, strategy: T) {
        viewModelScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main.immediate) {
                AlertDialogBuilder(DIALOG_TAG_PROGRESS).build()
            }
            if (downloadUri.isEmpty(context.contentResolver)) {
                withContext(Dispatchers.Main.immediate) {
                    showSnackbar(
                        TextMessage(
                            R.string.download_snackbar_action_view_error_format,
                            downloadUri.toString()
                        )
                    )
                }
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