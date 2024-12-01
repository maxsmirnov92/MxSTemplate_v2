package net.maxsmr.feature.download.ui.adapter

import android.annotation.SuppressLint
import android.net.Uri
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.v2.adapterDelegate
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseAdapterData
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter.DragAndDropViewHolder.Companion.createWithDraggable
import net.maxsmr.commonutils.conversion.SizeUnit.BYTES
import net.maxsmr.commonutils.conversion.roundTime
import net.maxsmr.commonutils.format.TIME_UNITS_TO_EXCLUDE_DEFAULT
import net.maxsmr.commonutils.format.TimePluralFormat
import net.maxsmr.commonutils.format.decomposeTimeFormatted
import net.maxsmr.commonutils.format.formatSizeSingle
import net.maxsmr.commonutils.format.formatSpeedSize
import net.maxsmr.commonutils.gui.setTextOrGone
import net.maxsmr.commonutils.text.capFirstChar
import net.maxsmr.core.database.model.download.DownloadInfo
import net.maxsmr.core.network.exceptions.NoConnectivityException
import net.maxsmr.core.network.exceptions.NoPreferableConnectivityException
import net.maxsmr.core.network.exceptions.NoPreferableConnectivityException.PreferableType.CELLULAR
import net.maxsmr.core.network.exceptions.NoPreferableConnectivityException.PreferableType.WIFI
import net.maxsmr.feature.download.data.DownloadService
import net.maxsmr.feature.download.data.DownloadStateNotifier.DownloadState
import net.maxsmr.feature.download.data.manager.DownloadInfoResultData
import net.maxsmr.feature.download.ui.R
import net.maxsmr.feature.download.ui.databinding.ItemDownloadInfoBinding
import java.util.concurrent.TimeUnit

@SuppressLint("StringFormatInvalid")
fun downloadInfoAdapterDelegate(listener: DownloadListener) =
    adapterDelegate<DownloadInfoAdapterData, DownloadInfoAdapterData, BaseDraggableDelegationAdapter.DragAndDropViewHolder<DownloadInfoAdapterData>>(
        R.layout.item_download_info, createViewHolder = { it.createWithDraggable(R.id.containerDownloadInfo) }
    ) {

        val binding = ItemDownloadInfoBinding.bind(itemView)

        with(binding) {

            ibCancel.setOnClickListener {
                listener.onCancelDownload(item.downloadInfo)
            }
            ibRetry.setOnClickListener {
                listener.onRetryDownload(item.downloadInfo, item.params, item.state)
            }
            ibDetails.setOnClickListener {
                listener.onShowDownloadDetails(item.downloadInfo, item.params, item.state, ibDetails)
            }
            ibDelete.setOnClickListener {
                listener.onDeleteResource(item.id, item.downloadInfo.nameWithExt)
            }
            ibView.setOnClickListener {
                item.downloadInfo.localUri?.let {
                    listener.onViewResource(it, item.downloadInfo.mimeType)
                }
            }
            ibShare.setOnClickListener {
                item.downloadInfo.localUri?.let {
                    listener.onShareResource(it, item.downloadInfo.mimeType)
                }
            }

            bind {
                tvResourceName.text = item.downloadInfo.nameWithExt
                val state = item.state

                val hasProgress: Boolean
                val progress: Int
                val isIndeterminate: Boolean

                val statusInfoText: String
                var statusColorResId: Int = net.maxsmr.core.ui.R.color.textColorPrimary

                if (state != null) {
                    hasProgress = state is DownloadState.Loading || state is DownloadState.Success
                    // contentLength неизвестен - неопределённый ProgressBar
                    isIndeterminate = state is DownloadState.Loading && state.stateInfo.totalBytes == 0L

                    progress = when (state) {
                        is DownloadState.Loading -> {
                            if (!state.stateInfo.done) {
                                state.stateInfo.progressRounded.takeIf { it > 0 } ?: 0
                            } else {
                                100
                            }
                        }

                        is DownloadState.Success -> 100
                        else -> 0
                    }

                    statusInfoText = when (state) {
                        is DownloadState.Loading -> {

                            with(state.stateInfo) {

                                if (!done) {

                                    val parts = mutableListOf<CharSequence>()

                                    if (this.progress >= 0) {
                                        parts.add(
                                            context.getString(
                                                when (state.type) {
                                                    DownloadState.Loading.Type.UPLOADING -> R.string.download_status_uploading_percent_format
                                                    DownloadState.Loading.Type.DOWNLOADING -> R.string.download_status_downloading_percent_format
                                                    DownloadState.Loading.Type.STORING -> R.string.download_status_storing_percent_format
                                                },
                                                this.progress
                                            )
                                        )
                                    } else {
                                        parts.add(
                                            context.getString(
                                                when (state.type) {
                                                    DownloadState.Loading.Type.UPLOADING -> R.string.download_status_uploading
                                                    DownloadState.Loading.Type.DOWNLOADING -> R.string.download_status_downloading
                                                    DownloadState.Loading.Type.STORING -> R.string.download_status_storing
                                                }
                                            )
                                        )
                                    }
                                    if (currentBytes > 0) {
                                        val currentSizeFormatted = formatSizeSingle(
                                            currentBytes,
                                            BYTES,
                                            setOf(BYTES),
                                            precision = 2,
                                        )

                                        if (currentSizeFormatted != null) {
                                            val sizePart = if (totalBytes > 0) {
                                                val totalSizeFormatted = formatSizeSingle(
                                                    totalBytes,
                                                    BYTES,
                                                    setOf(BYTES),
                                                    precision = 2,
                                                )
                                                if (totalSizeFormatted != null) {
                                                    context.getString(
                                                        R.string.download_status_size_with_total_text_format,
                                                        currentSizeFormatted.get(context),
                                                        totalSizeFormatted.get(context)
                                                    )
                                                } else {
                                                    context.getString(
                                                        R.string.download_status_size_text_format,
                                                        currentSizeFormatted.get(context)
                                                    )
                                                }
                                            } else {
                                                context.getString(
                                                    R.string.download_status_size_text_format,
                                                    currentSizeFormatted.get(context)
                                                )
                                            }
                                            parts.add(sizePart)
                                        }
                                    }
                                    if (speed > 0) {
                                        val speedFormatted = formatSpeedSize(
                                            speed,
                                            BYTES,
                                            setOf(BYTES),
                                            precision = 2,
                                            timeUnit = TimeUnit.SECONDS
                                        )
                                        speedFormatted?.get(context)?.let {
                                            parts.add(it)
                                        }
                                    }
                                    if (elapsedTime > 0) {
                                        val elapsedTimeFormatted = decomposeTimeFormatted(
                                            roundTime(elapsedTime, TimeUnit.SECONDS),
                                            TimeUnit.NANOSECONDS,
                                            TimePluralFormat.NORMAL_WITH_VALUE,
                                            timeUnitsToExclude = TIME_UNITS_TO_EXCLUDE_DEFAULT
                                        ).joinToString {
                                            it.get(context)
                                        }

                                        if (elapsedTimeFormatted.isNotEmpty()) {
                                            val timePart = if (estimatedTime > 0) {
                                                val estimatedTimeFormatted = decomposeTimeFormatted(
                                                    roundTime(estimatedTime, TimeUnit.SECONDS),
                                                    TimeUnit.NANOSECONDS,
                                                    TimePluralFormat.NORMAL_WITH_VALUE,
                                                    timeUnitsToExclude = TIME_UNITS_TO_EXCLUDE_DEFAULT
                                                ).joinToString {
                                                    it.get(context)
                                                }
                                                if (estimatedTimeFormatted.isNotEmpty()) {
                                                    context.getString(
                                                        R.string.download_status_time_with_estimated_text_format,
                                                        elapsedTimeFormatted,
                                                        estimatedTimeFormatted
                                                    )
                                                } else {
                                                    context.getString(
                                                        R.string.download_status_time_text_format,
                                                        elapsedTimeFormatted
                                                    )
                                                }
                                            } else {
                                                context.getString(
                                                    R.string.download_status_time_text_format,
                                                    elapsedTimeFormatted
                                                )
                                            }
                                            parts.add(timePart)
                                        }
                                    }

                                    parts.mapIndexedNotNull { index, s ->
                                        (if (index == 0) {
                                            s.capFirstChar()
                                        } else {
                                            s
                                        }).takeIf { it.isNotEmpty() }
                                    }.joinToString(separator = " | ")
                                } else {
                                    context.getString(
                                        when (state.type) {
                                            DownloadState.Loading.Type.UPLOADING -> R.string.download_status_finalize_uploading
                                            DownloadState.Loading.Type.DOWNLOADING -> R.string.download_status_finalize_downloading
                                            DownloadState.Loading.Type.STORING -> R.string.download_status_finalize_storing
                                        }
                                    )
                                }
                            }
                        }

                        is DownloadState.Success -> {
                            statusColorResId = R.color.textColorDownloadSuccess
                            context.getString(R.string.download_status_success)
                        }

                        is DownloadState.Failed -> {
                            statusColorResId = R.color.textColorDownloadFailed

                            state.e.message?.takeIf { it.isNotEmpty() }?.let {
                                context.getString(R.string.download_status_failed_format, it)
                            } ?: context.getString(R.string.download_status_failed_unknown)
                        }

                        is DownloadState.Cancelled -> {
                            statusColorResId = R.color.textColorDownloadCancelled
                            context.getString(R.string.download_status_cancelled)
                        }

                        else -> {
                            throw IllegalStateException("Unknown DownloadState: $state")
                        }
                    }
                } else {
                    hasProgress = true
                    progress = 0
                    isIndeterminate = true
                    statusInfoText = context.getString(R.string.download_status_requesting)
                }

                pbDownload.isVisible = hasProgress
                pbDownload.progress = progress
                pbDownload.isIndeterminate = isIndeterminate

                tvStatusInfo.setTextOrGone(statusInfoText)
                tvStatusInfo.setTextColor(ContextCompat.getColor(context, statusColorResId))

                ibCancel.isVisible = state is DownloadState.Loading || state == null // для этапа выполнения запроса
                ibRetry.isVisible = state !is DownloadState.Loading && state != null
                containerSuccessButtons.isVisible =
                    state is DownloadState.Success && state.downloadInfo.localUri != null
            }
        }
    }

data class DownloadInfoAdapterData(
    val data: DownloadInfoResultData,
) : BaseAdapterData {

    val id = data.id

    val params = data.params

    val downloadInfo = data.downloadInfo

    val state = data.state

    override fun areContentsSame(other: BaseAdapterData): Boolean {
        return if (other !is DownloadInfoAdapterData) {
            false
        } else {
            this.data == other.data
        }
    }

    override fun isSame(other: BaseAdapterData): Boolean = id == (other as? DownloadInfoAdapterData)?.id
}

interface DownloadListener {

    // TODO пауза

    fun onCancelDownload(downloadInfo: DownloadInfo)

    fun onRetryDownload(
        downloadInfo: DownloadInfo,
        params: DownloadService.Params,
        state: DownloadState?,
    )

    fun onShowDownloadDetails(
        downloadInfo: DownloadInfo,
        params: DownloadService.Params,
        state: DownloadState?,
        anchorView: View,
    )

    fun onDeleteResource(downloadId: Long, name: String)

    fun onViewResource(downloadUri: Uri, mimeType: String)

    fun onShareResource(downloadUri: Uri, mimeType: String)
}