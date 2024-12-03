package net.maxsmr.feature.download.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.media.length
import net.maxsmr.core.ProgressListener
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.database.model.download.DownloadInfo
import java.io.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadStateNotifier @Inject constructor() {

    private val scope = CoroutineScope(Dispatchers.Default + Job())

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

    fun onDownloadStarting(downloadInfo: DownloadInfo, params: DownloadService.Params) {
        scope.launch {
            _downloadStartEvents.emit(DownloadStartInfo(params, true, downloadInfo))
        }
    }

    fun onDownloadRetry(params: DownloadService.Params) {
        scope.launch {
            _downloadRetryEvents.emit(params)
        }
    }

    fun onDownloadProcessing(
        type: DownloadState.Loading.Type,
        stateInfo: ProgressListener.ProgressStateInfo,
        downloadInfo: DownloadInfo,
        params: DownloadService.Params,
    ) {
        scope.launch {
            _downloadStateEvents.emit(DownloadState.Loading(type, stateInfo, downloadInfo, params))
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
        e: Exception,
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
        val downloadInfo: DownloadInfo? = null,
    ): Serializable {

        override fun toString(): String {
            return "DownloadStartInfo(params=$params, isStarted=$isStarted, downloadInfo=$downloadInfo)"
        }
    }

    sealed class DownloadState(
        val downloadInfo: DownloadInfo,
        val params: DownloadService.Params,
        val oldParams: DownloadService.Params,
    ) : Serializable {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DownloadState) return false

            if (downloadInfo != other.downloadInfo) return false
            if (params != other.params) return false
            return oldParams == other.oldParams
        }

        override fun hashCode(): Int {
            var result = downloadInfo.hashCode()
            result = 31 * result + params.hashCode()
            result = 31 * result + oldParams.hashCode()
            return result
        }

        class Loading(
            val type: Type,
            val stateInfo: ProgressListener.ProgressStateInfo,
            downloadInfo: DownloadInfo,
            params: DownloadService.Params,
        ) : DownloadState(downloadInfo, params, params) {

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Loading) return false
                if (!super.equals(other)) return false

                if (type != other.type) return false
                return stateInfo == other.stateInfo
            }

            override fun hashCode(): Int {
                var result = super.hashCode()
                result = 31 * result + type.hashCode()
                result = 31 * result + stateInfo.hashCode()
                return result
            }

            override fun toString(): String {
                return "DownloadState.Loading(type=$type, stateInfo=$stateInfo, downloadInfo=$downloadInfo, params=$params, oldParams=$oldParams)"
            }

            enum class Type {

                UPLOADING,
                DOWNLOADING,
                STORING
            }
        }

        class Success(
            downloadInfo: DownloadInfo,
            params: DownloadService.Params,
            oldParams: DownloadService.Params,
        ) : DownloadState(downloadInfo, params, oldParams) {

            val resourceLength get() = downloadInfo.localUri?.length(baseApplicationContext.contentResolver) ?: 0

            override fun toString(): String {
                return "DownloadState.Success(downloadInfo=$downloadInfo, params=$params, oldParams=$oldParams)"
            }
        }

        class Failed(
            val e: Exception,
            downloadInfo: DownloadInfo,
            params: DownloadService.Params,
            oldParams: DownloadService.Params,
        ) : DownloadState(downloadInfo, params, oldParams) {

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Failed) return false
                if (!super.equals(other)) return false

                return e == other.e
            }

            override fun hashCode(): Int {
                var result = super.hashCode()
                result = 31 * result + e.hashCode()
                return result
            }

            override fun toString(): String {
                return "DownloadState.Failed(e=$e, downloadInfo=$downloadInfo, params=$params, oldParams=$oldParams)"
            }
        }

        class Cancelled(
            downloadInfo: DownloadInfo,
            params: DownloadService.Params,
            oldParams: DownloadService.Params,
        ) : DownloadState(downloadInfo, params, oldParams) {

            override fun toString(): String {
                return "DownloadState.Cancelled(downloadInfo=$downloadInfo, params=$params, oldParams=$oldParams)"
            }
        }
    }
}