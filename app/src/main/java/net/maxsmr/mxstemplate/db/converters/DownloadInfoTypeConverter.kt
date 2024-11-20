package net.maxsmr.mxstemplate.db.converters

import androidx.room.TypeConverter
import net.maxsmr.commonutils.asByteArray
import net.maxsmr.commonutils.asObject

import net.maxsmr.core.database.model.download.DownloadInfo

class DownloadInfoTypeConverter {

    @TypeConverter
    fun convert(status: DownloadInfo.Status): ByteArray {
//        return baseJson.encodeToStringOrNull(status) ?: throw RuntimeException("Failed convert from DownloadInfo.Status")
        return status.asByteArray() ?: throw RuntimeException("Failed convert from DownloadInfo.Status")
    }

    @TypeConverter
    fun convertDownloadInfoStatus(array: ByteArray): DownloadInfo.Status {
//        return baseJson.decodeFromStringOrNull(json) ?: DownloadInfo.Status.Error()
        return array.asObject() ?: DownloadInfo.Status.Error()
    }
}