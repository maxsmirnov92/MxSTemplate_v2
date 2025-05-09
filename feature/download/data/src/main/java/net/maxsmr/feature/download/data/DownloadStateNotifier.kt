package net.maxsmr.feature.download.data

import android.content.Context
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import net.maxsmr.commonutils.media.length
import net.maxsmr.core.ProgressListener
import net.maxsmr.core.database.model.download.DownloadInfo
import java.io.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadStateNotifier @Inject constructor() {

    val event: SharedFlow<DownloadNotifierEvent> by lazy {
        _event.asSharedFlow()
    }

    private val _event = MutableSharedFlow<DownloadNotifierEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun onDownloadNotStarted(params: DownloadService.Params) {
        _event.tryEmit(DownloadNotifierEvent.NotStarted(params))
    }

    fun onDownloadStarting(downloadInfo: DownloadInfo, params: DownloadService.Params) {
        _event.tryEmit(DownloadNotifierEvent.Starting(params, downloadInfo))
    }

    fun onDownloadRetry(params: DownloadService.Params) {
        _event.tryEmit(DownloadNotifierEvent.Retry(params))
    }

    fun onDownloadProcessing(
        type: DownloadState.Loading.Type,
        stateInfo: ProgressListener.ProgressStateInfo,
        downloadInfo: DownloadInfo,
        params: DownloadService.Params,
    ) {
        _event.tryEmit(DownloadNotifierEvent.State(DownloadState.Loading(type, stateInfo, downloadInfo, params)))
    }

    fun onDownloadSuccess(
        downloadInfo: DownloadInfo,
        params: DownloadService.Params,
        oldParams: DownloadService.Params,
    ) {
        _event.tryEmit(DownloadNotifierEvent.State(DownloadState.Success(downloadInfo, params, oldParams)))
    }

    fun onDownloadFailed(
        downloadInfo: DownloadInfo,
        params: DownloadService.Params,
        oldParams: DownloadService.Params,
        e: Exception,
    ) {
        _event.tryEmit(DownloadNotifierEvent.State(DownloadState.Failed(e, downloadInfo, params, oldParams)))
    }

    fun onDownloadCancelled(
        downloadInfo: DownloadInfo,
        params: DownloadService.Params,
        oldParams: DownloadService.Params,
    ) {
        _event.tryEmit(DownloadNotifierEvent.State(DownloadState.Cancelled(downloadInfo, params, oldParams)))
    }

    sealed interface DownloadNotifierEvent {

        sealed interface Start : DownloadNotifierEvent {
            val params: DownloadService.Params
        }

        class NotStarted(
            override val params: DownloadService.Params,
        ) : Start {

            override fun toString(): String {
                return "NotStarted(params=$params)"
            }
        }

        class Starting(
            override val params: DownloadService.Params,
            val downloadInfo: DownloadInfo,
        ) : Start {

            override fun toString(): String {
                return "Starting(params=$params, downloadInfo=$downloadInfo)"
            }
        }

        class Retry(
            val params: DownloadService.Params,
        ) : DownloadNotifierEvent {

            override fun toString(): String {
                return "Retry(params=$params)"
            }
        }

        class State(
            val state: DownloadState,
        ) : DownloadNotifierEvent {

            override fun toString(): String {
                return "State(state=$state)"
            }
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

            fun getResourceLength(context: Context) = downloadInfo.localUri?.length(context.contentResolver) ?: 0

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