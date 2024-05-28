package net.maxsmr.feature.download.ui

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.connection.ConnectionManager
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.asYesNoDialog
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.feature.download.data.DownloadService
import net.maxsmr.feature.download.data.DownloadStateNotifier
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.download.data.manager.DownloadInfoResultData
import net.maxsmr.feature.download.data.manager.DownloadManager
import net.maxsmr.feature.download.ui.adapter.DownloadInfoAdapterData

class DownloadsStateViewModel @AssistedInject constructor(
    @Assisted state: SavedStateHandle,
    @Assisted private val downloadsViewModel: DownloadsViewModel,
    private val manager: DownloadManager,
) : BaseHandleableViewModel(state) {

    override val connectionManager: ConnectionManager = ConnectionManager(snackbarQueue)

    val queueNames = MutableLiveData<List<String>>()

    val allItems = MutableLiveData<List<DownloadInfoAdapterData>>()

    val currentItems = MutableLiveData<List<DownloadInfoAdapterData>>()

    val anyCanBeCancelled = currentItems.map { it.any { item -> item.downloadInfo.isLoading } }

    val queryNameFilter = MutableLiveData<String>()

    override fun onInitialized() {
        super.onInitialized()
        viewModelScope.launch {
            manager.downloadsPendingParams.collect {
                queueNames.postValue(it.map { params -> params.targetResourceName })
            }
        }
        viewModelScope.launch {
            manager.resultItems.collect {
                allItems.postValue(it.map { item -> DownloadInfoAdapterData(item) })
                currentItems.postValue(it.mapWithFilterByName(queryNameFilter.value.orEmpty()))
            }
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

    override fun handleAlerts(context: Context, delegate: AlertFragmentDelegate<*>) {
        super.handleAlerts(context, delegate)
        delegate.bindAlertDialog(DIALOG_TAG_CLEAR_QUEUE) {
            it.asYesNoDialog(context)
        }
        delegate.bindAlertDialog(DIALOG_TAG_CANCEL_ALL) {
            it.asYesNoDialog(context)
        }
        delegate.bindAlertDialog(DIALOG_TAG_RETRY_IF_SUCCESS) {
            it.asYesNoDialog(context)
        }
    }

    fun onClearQueue() {
        if (queueNames.value?.isNotEmpty() == true) {
            showYesNoDialog(
                DIALOG_TAG_CLEAR_QUEUE,
                TextMessage(R.string.download_alert_clear_queue_message),
                TextMessage(R.string.download_alert_confirm_title),
                onPositiveSelect = { manager.removeAllPending() }
            )
        }
    }

    fun onCancelAllDownloads() {
        if (anyCanBeCancelled.value == true) {
            showYesNoDialog(
                DIALOG_TAG_CANCEL_ALL,
                TextMessage(R.string.download_alert_cancel_all_message),
                TextMessage(R.string.download_alert_confirm_title),
                onPositiveSelect = { DownloadService.cancelAll() }
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
                TextMessage(R.string.download_alert_retry_if_success_message),
                TextMessage(R.string.download_alert_confirm_title),
                onPositiveSelect = { manager.retryDownload(downloadId, params) }
            )
        } else {
            manager.retryDownload(downloadId, params)
        }
    }

    fun onClearFinished() {
        manager.removeAllFinished()
    }


    fun onCancelDownload(id: Long) {
        DownloadService.cancel(id)
    }

    fun onRemoveFinishedDownload(id: Long) {
        manager.removeFinished(id)
    }

    fun onNameQueryFilterChanged(value: String?) {
        queryNameFilter.value = value.orEmpty()/*.trim()*/
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

    @AssistedFactory
    interface Factory {

        fun create(
            state: SavedStateHandle,
            downloadsViewModel: DownloadsViewModel,
        ): DownloadsStateViewModel
    }

    companion object {

        const val DIALOG_TAG_CLEAR_QUEUE = "clear_queue"
        const val DIALOG_TAG_CANCEL_ALL = "cancel_all"
        const val DIALOG_TAG_RETRY_IF_SUCCESS = "retry_if_success"
    }
}