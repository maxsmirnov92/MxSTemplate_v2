package net.maxsmr.mxstemplate.db.converters;

import androidx.room.TypeConverter

import net.maxsmr.core.database.model.download.DownloadInfo
import net.maxsmr.core.utils.asByteArray
import net.maxsmr.core.utils.asObject


class DownloadInfoTypeConverter {

    @TypeConverter
    fun convert(status: DownloadInfo.Status): ByteArray {
//        return baseJson.encodeToStringOrNull(status) ?: throw RuntimeException("Failed convert from DownloadInfo.Status")
        return status.asByteArray() ?: throw RuntimeException("Failed convert from DownloadInfo.Status")
    }

    @TypeConverter
    fun convertDownloadInfoStatus(array: ByteArray): DownloadInfo.Status {
//        return baseJson.decodeFromStringOrNull(json) ?: throw RuntimeException("Failed convert to DownloadInfo.Status")
        return array.asObject() ?: throw RuntimeException("Failed convert to DownloadInfo.Status")
    }
}