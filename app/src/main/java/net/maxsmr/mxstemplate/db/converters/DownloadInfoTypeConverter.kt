package net.maxsmr.mxstemplate.db.converters;

import androidx.room.TypeConverter
import net.maxsmr.commonutils.model.SerializationUtils.fromByteArray
import net.maxsmr.commonutils.model.SerializationUtils.toByteArray
import net.maxsmr.core.database.model.download.DownloadInfo
import net.maxsmr.core.utils.decodeFromStringOrNull
import net.maxsmr.core.utils.encodeToStringOrNull
import net.maxsmr.mxstemplate.baseJson

class DownloadInfoTypeConverter {

    @TypeConverter
    fun convert(status: DownloadInfo.Status): ByteArray {
//        return baseJson.encodeToStringOrNull(status) ?: throw RuntimeException("Failed convert from DownloadInfo.Status")
        return toByteArray(status) ?: throw RuntimeException("Failed convert from DownloadInfo.Status")
    }

    @TypeConverter
    fun convertDownloadInfoStatus(array: ByteArray): DownloadInfo.Status {
//        return baseJson.decodeFromStringOrNull(json) ?: throw RuntimeException("Failed convert to DownloadInfo.Status")
        return fromByteArray(DownloadInfo.Status::class.java, array) ?: throw RuntimeException("Failed convert to DownloadInfo.Status")
    }
}