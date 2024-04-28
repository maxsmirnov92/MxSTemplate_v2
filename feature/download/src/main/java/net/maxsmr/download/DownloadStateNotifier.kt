package net.maxsmr.download

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import net.maxsmr.core.database.model.download.DownloadInfo
import net.maxsmr.core.di.AppDispatchers
import net.maxsmr.core.di.ApplicationScope
import net.maxsmr.core.di.Dispatcher
import java.io.Serializable
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadStateNotifier @Inject constructor(
    @Dispatcher(AppDispatchers.Default)
    private val defaultDispatcher: CoroutineDispatcher,
) {

    private val scope = CoroutineScope(defaultDispatcher + Job())

    private val _downloadStartEvents = MutableSharedFlow<DownloadStartInfo>()

    val downloadStartEvents = _downloadStartEvents.asSharedFlow()

    private val _downloadRetryEvents = MutableSharedFlow<DownloadService.Params>()

    val downloadRetryEvents = _downloadRetryEvents.asSharedFlow()

    private val _downloadStateEvents = MutableSharedFlow<DownloadState>()

    val downloadStateEvents = _downloadStateEvents.asSharedFlow()

    fun onDownloadNotStarted(params: DownloadService.Params) {
        scope.launch {
            _downloadStartEvents.emit(DownloadStartInfo(params, false))
        }
    }

    fun onDownloadStarting(params: DownloadService.Params) {
        scope.launch {
            _downloadStartEvents.emit(DownloadStartInfo(params, true))
        }
    }

    fun onDownloadRetry(params: DownloadService.Params) {
        scope.launch {
            _downloadRetryEvents.emit(params)
        }
    }

    fun onDownloadProcessing(
        downloadInfo: DownloadInfo,
        params: DownloadService.Params,
        currentBytes: Long,
        totalBytes: Long,
    ) {
        scope.launch {
            _downloadStateEvents.emit(DownloadState.Loading(currentBytes, totalBytes, downloadInfo, params))
        }
    }

    fun onDownloadSuccess(
        downloadInfo: DownloadInfo,
        params: DownloadService.Params,
        oldParams: DownloadService.Params,
    ) {
        scope.launch {
            _downloadStateEvents.emit(DownloadState.Success(downloadInfo, params, oldParams))
        }
    }

    fun onDownloadFailed(
        downloadInfo: DownloadInfo,
        params: DownloadService.Params,
        oldParams: DownloadService.Params,
        e: Exception?,
    ) {
        scope.launch {
            _downloadStateEvents.emit(DownloadState.Failed(e, downloadInfo, params, oldParams))
        }
    }

    fun onDownloadCancelled(
        downloadInfo: DownloadInfo,
        params: DownloadService.Params,
        oldParams: DownloadService.Params,
    ) {
        scope.launch {
            _downloadStateEvents.emit(DownloadState.Cancelled(downloadInfo, params, oldParams))
        }
    }

    class DownloadStartInfo(
        val params: DownloadService.Params,
        val isStarted: Boolean,
    )

    sealed class DownloadState(
        val downloadInfo: DownloadInfo,
        val params: DownloadService.Params,
        val oldParams: DownloadService.Params,
    ) : Serializable {

        class Loading(
            val currentBytes: Long,
            val totalBytes: Long,
            downloadInfo: DownloadInfo,
            params: DownloadService.Params,
        ) : DownloadState(downloadInfo, params, params)

        class Success(
            downloadInfo: DownloadInfo,
            params: DownloadService.Params,
            oldParams: DownloadService.Params,
        ) : DownloadState(downloadInfo, params, oldParams)

        class Failed(
            val e: Exception?,
            downloadInfo: DownloadInfo,
            params: DownloadService.Params,
            oldParams: DownloadService.Params,
        ) : DownloadState(downloadInfo, params, oldParams)

        class Cancelled(
            downloadInfo: DownloadInfo,
            params: DownloadService.Params,
            oldParams: DownloadService.Params,
        ) : DownloadState(downloadInfo, params, oldParams)

        override fun toString(): String {
            return "DownloadState(downloadInfo=$downloadInfo, params=$params, oldParams=$oldParams)"
        }
    }
}