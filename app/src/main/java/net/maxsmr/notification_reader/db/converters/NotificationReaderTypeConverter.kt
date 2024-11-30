package net.maxsmr.notification_reader.db.converters

import androidx.room.TypeConverter
import kotlinx.serialization.SerializationException
import net.maxsmr.commonutils.asByteArray
import net.maxsmr.commonutils.asObject

import net.maxsmr.core.database.model.notification_reader.NotificationReaderEntity

class NotificationReaderTypeConverter {

    @TypeConverter
    fun convert(status: NotificationReaderEntity.Status): ByteArray {
        return status.asByteArray() ?: throw RuntimeException("Failed convert from NotificationReaderEntity.Status")
    }

    @TypeConverter
    fun convertNotificationReaderStatus(array: ByteArray): NotificationReaderEntity.Status {
        return array.asObject() ?: NotificationReaderEntity.Failed(System.currentTimeMillis(), SerializationException())
    }
}