package net.maxsmr.feature.download.data.manager

import net.maxsmr.core.database.model.download.DownloadInfo
import net.maxsmr.feature.download.data.DownloadService
import net.maxsmr.feature.download.data.DownloadStateNotifier
import java.io.Serializable

data class DownloadInfoResultData(
    val params: DownloadService.Params,
    val downloadInfo: DownloadInfo,
    val state: DownloadStateNotifier.DownloadState?,
): Serializable {

    init {
        if (state != null && state.downloadInfo.id != downloadInfo.id) {
            throw IllegalArgumentException("DownloadInfo ids not match: ${downloadInfo.id}, ${state.downloadInfo.id}")
        }
    }

    val id = downloadInfo.id

    val length get() = if (state is DownloadStateNotifier.DownloadState.Success) {
        state.getLength()
    } else {
        0
    }
}