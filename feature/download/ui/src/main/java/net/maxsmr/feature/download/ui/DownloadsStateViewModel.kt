package net.maxsmr.feature.download.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.feature.download.data.DownloadService
import net.maxsmr.feature.download.data.DownloadStateNotifier
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.download.data.manager.DownloadManager
import net.maxsmr.feature.download.ui.adapter.DownloadInfoAdapterData

class DownloadsStateViewModel @AssistedInject constructor(
    @Assisted state: SavedStateHandle,
    @Assisted private val downloadsViewModel: DownloadsViewModel,
    private val manager: DownloadManager,
) : BaseViewModel(state) {

    val queueNames = MutableLiveData<List<String>>()

    val downloadItems = MutableLiveData<List<DownloadInfoAdapterData>>()

    override fun onInitialized() {
        super.onInitialized()
        viewModelScope.launch {
            manager.downloadsPendingParams.collect {
                queueNames.postValue(it.map { params -> params.targetResourceName })
            }
        }
        viewModelScope.launch {
            manager.resultItems.collect {
                downloadItems.postValue(it.map { item -> DownloadInfoAdapterData(item) })
            }
        }
    }

    fun onClearQueue() {
        AlertBuilder(DIALOG_TAG_CLEAR_QUEUE)
            .setTitle(R.string.download_alert_confirm_title)
            .setMessage(R.string.download_alert_clear_queue_message)
            .setAnswers(
                Alert.Answer(android.R.string.yes).onSelect {
                    manager.removeAllPending()
                },
                Alert.Answer(android.R.string.no)
            )
            .build()
    }

    fun onRetryDownload(
        downloadId: Long,
        params: DownloadService.Params,
        state: DownloadStateNotifier.DownloadState?,
    ) {
        if (state is DownloadStateNotifier.DownloadState.Success) {
            AlertBuilder(DIALOG_TAG_RETRY_DOWNLOAD_IF_SUCCESS)
                .setTitle(R.string.download_alert_confirm_title)
                .setMessage(R.string.download_alert_retry_download_if_success_message)
                .setAnswers(
                    Alert.Answer(android.R.string.yes).onSelect {
                        manager.retryDownload(downloadId, params)
                    },
                    Alert.Answer(android.R.string.no)
                )
                .build()
        } else {
            manager.retryDownload(downloadId, params)
        }
    }

    fun onClearFinished() {
        manager.removeAllFinished()
    }

    fun onCancelAllDownloads() {
        DownloadService.cancelAll()
    }

    fun onCancelDownload(id: Long) {
        DownloadService.cancel(id)
    }

    fun onRemoveFinishedDownload(id: Long) {
        manager.removeFinished(id)
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
        const val DIALOG_TAG_RETRY_DOWNLOAD_IF_SUCCESS = "retry_download_if_success"
    }
}