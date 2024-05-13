package net.maxsmr.feature.download.ui.adapter

import android.annotation.SuppressLint
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
import net.maxsmr.commonutils.format.decomposeSizeFormatted
import net.maxsmr.commonutils.format.decomposeTimeFormatted
import net.maxsmr.commonutils.format.formatSpeedSize
import net.maxsmr.commonutils.gui.setTextOrGone
import net.maxsmr.commonutils.text.capFirstChar
import net.maxsmr.core.database.model.download.DownloadInfo
import net.maxsmr.feature.download.data.DownloadService
import net.maxsmr.feature.download.data.DownloadStateNotifier.DownloadState
import net.maxsmr.feature.download.data.manager.DownloadInfoResultData
import net.maxsmr.feature.download.ui.R
import net.maxsmr.feature.download.ui.databinding.ItemDownloadInfoBinding
import java.util.concurrent.TimeUnit

@SuppressLint("StringFormatInvalid")
fun downloadInfoDelegateAdapter(listener: DownloadListener) =
    adapterDelegate<DownloadInfoAdapterData, DownloadInfoAdapterData, BaseDraggableDelegationAdapter.DragAndDropViewHolder<DownloadInfoAdapterData>>(
        R.layout.item_download_info, createViewHolder = { it.createWithDraggable(R.id.containerDownloadInfo) }
    ) {

        val binding = ItemDownloadInfoBinding.bind(itemView)

        with(binding) {
            ibCancel.setOnClickListener {
                listener.onCancelDownload(item.downloadInfo)
            }
            ibRetry.setOnClickListener {
                listener.onRetryDownload(item.downloadInfo, item.params)
            }

            bind {
                tvResourceName.text = item.downloadInfo.nameWithExt
                val state = item.state

                var isIndeterminate = true
                val statusInfo: String
                var statusColorResId: Int = net.maxsmr.core.ui.R.color.textColorPrimary

                if (state != null) {
                    pbDownload.isVisible = state is DownloadState.Loading || state is DownloadState.Success

                    pbDownload.progress = when (state) {
                        is DownloadState.Loading -> {
                            // contentLength неизвестен - неопределённый ProgressBar
                            val progress = if (!state.stateInfo.done) {
                                state.stateInfo.progressRounded.takeIf { it > 0 } ?: 0
                            } else {
                                100
                            }
                            isIndeterminate = false
                            progress
                        }

                        is DownloadState.Success -> {
                            isIndeterminate = false
                            100
                        }

                        else -> 0
                    }

                    statusInfo = when (state) {
                        is DownloadState.Loading -> {

                            with(state.stateInfo) {
                                if (!done) {
                                    val parts = mutableListOf<CharSequence>()

                                    if (progress >= 0) {
                                        parts.add(
                                            context.getString(
                                                R.string.download_status_percent_text_format,
                                                progress
                                            )
                                        )
                                    }
                                    if (bytesRead > 0) {
                                        val currentSizeFormatted = decomposeSizeFormatted(
                                            bytesRead,
                                            BYTES,
                                            setOf(BYTES),
                                            formatWithValue = true
                                        ).joinToString { it.get(context) }

                                        if (currentSizeFormatted.isNotEmpty()) {
                                            val sizePart = if (contentLength > 0) {
                                                val totalSizeFormatted = decomposeSizeFormatted(
                                                    contentLength,
                                                    BYTES,
                                                    setOf(BYTES),
                                                    formatWithValue = true
                                                ).joinToString { it.get(context) }
                                                if (totalSizeFormatted.isNotEmpty()) {
                                                    context.getString(
                                                        R.string.download_status_size_with_total_text_format,
                                                        currentSizeFormatted,
                                                        totalSizeFormatted
                                                    )
                                                } else {
                                                    context.getString(
                                                        R.string.download_status_size_text_format,
                                                        currentSizeFormatted
                                                    )
                                                }
                                            } else {
                                                context.getString(
                                                    R.string.download_status_size_text_format,
                                                    currentSizeFormatted
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
                                    context.getString(R.string.download_status_finalizing)
                                }
                            }
                        }

                        is DownloadState.Success -> {
                            statusColorResId = R.color.textColorDownloadSuccess
                            context.getString(R.string.download_status_success)
                        }

                        is DownloadState.Failed -> {
                            statusColorResId = R.color.textColorDownloadFailed
                            context.getString(R.string.download_status_failed_format, state.e?.message)
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
                    pbDownload.isVisible = true
                    statusInfo = context.getString(R.string.download_status_requesting)
                }
                pbDownload.isIndeterminate = isIndeterminate
                tvStatusInfo.setTextOrGone(statusInfo)
                tvStatusInfo.setTextColor(ContextCompat.getColor(context, statusColorResId))

                ibCancel.isVisible = state is DownloadState.Loading || state == null // для этапа выполнения запроса
                ibRetry.isVisible = state is DownloadState.Failed || state is DownloadState.Cancelled
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

    fun onRetryDownload(downloadInfo: DownloadInfo, params: DownloadService.Params)
}