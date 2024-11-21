package net.maxsmr.notification_reader.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.maxsmr.core.database.dao.download.DownloadsDao
import net.maxsmr.core.database.dao.notification_reader.NotificationReaderDao
import net.maxsmr.core.database.model.PrimitiveTypeConverter
import net.maxsmr.core.database.model.download.DownloadInfo
import net.maxsmr.core.database.model.notification_reader.NotificationReaderEntity
import net.maxsmr.notification_reader.db.converters.DownloadInfoTypeConverter
import net.maxsmr.notification_reader.db.converters.NotificationReaderTypeConverter

@Database(
    entities = [DownloadInfo::class, NotificationReaderEntity::class],
    version = 1
)
@TypeConverters(PrimitiveTypeConverter::class,
    DownloadInfoTypeConverter::class,
    NotificationReaderTypeConverter::class)
abstract class AppDataBase: RoomDatabase() {

    abstract fun downloadsDao(database: AppDataBase): DownloadsDao

    abstract fun notificationReaderDao(database: AppDataBase): NotificationReaderDao
}