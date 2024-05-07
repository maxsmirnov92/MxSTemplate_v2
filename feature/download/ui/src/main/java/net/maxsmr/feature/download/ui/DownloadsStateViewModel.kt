package net.maxsmr.feature.download.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.feature.download.data.DownloadService
import net.maxsmr.feature.download.data.DownloadStateNotifier
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.download.data.manager.DownloadManager
import net.maxsmr.feature.download.ui.adapter.DownloadInfoAdapterData
import javax.inject.Inject

class DownloadsStateViewModel @AssistedInject constructor(
    @Assisted state: SavedStateHandle,
    @Assisted private val downloadsViewModel: DownloadsViewModel,
    private val manager: DownloadManager,
    private val notifier: DownloadStateNotifier,
) : BaseViewModel(state) {

    val queueNames = MutableLiveData<List<String>>()

    val downloadItems = MutableLiveData<List<DownloadInfoAdapterData>>()

    override fun onInitialized() {
        super.onInitialized()
        viewModelScope.launch {
            manager.downloadsPendingParams.collect {
                queueNames.value = it.map { params -> params.targetResourceName }
            }
        }
        viewModelScope.launch {
            notifier.downloadStartEvents.collect {
                if (it.isStarted) {
                    it.downloadInfo?.let { downloadInfo ->
                        DownloadInfoAdapterData(it.params, downloadInfo, null).refreshWith()
                    }
                }
            }
        }
        viewModelScope.launch {
            notifier.downloadStateEvents.collect {
                DownloadInfoAdapterData(it.params, it.downloadInfo, it).refreshWith()
            }
        }
    }

    fun onCancelDownload(id: Long) {
        DownloadService.cancel(id)
    }

    fun onRetryDownload(id: Long, params: DownloadService.Params) {
        val currentDownloads = downloadItems.value?.toMutableList() ?: mutableListOf()
        if (currentDownloads.removeIf { it.id == id }) {
            downloadItems.value = currentDownloads
        }
        downloadsViewModel.download(params)
    }

    private fun DownloadInfoAdapterData.refreshWith() {
        var has = false
        val currentDownloads = downloadItems.value ?: emptyList()
        val newDownloads = currentDownloads.map { currentItem ->
            if (currentItem.downloadInfo.id == downloadInfo.id) {
                has = true
                this
            } else {
                currentItem
            }
        }.toMutableList()
        if (!has) {
            newDownloads.add(this)
        }
        downloadItems.value = newDownloads
    }

    @AssistedFactory
    interface Factory {

        fun create(
            state: SavedStateHandle,
            downloadsViewModel: DownloadsViewModel,
        ): DownloadsStateViewModel
    }
}