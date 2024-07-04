package net.maxsmr.justupdownloadit.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.maxsmr.core.database.dao.download.DownloadsDao
import net.maxsmr.core.database.model.PrimitiveTypeConverter
import net.maxsmr.core.database.model.download.DownloadInfo
import net.maxsmr.justupdownloadit.db.converters.DownloadInfoTypeConverter

@Database(
    entities = [DownloadInfo::class],
    version = 1
)
@TypeConverters(PrimitiveTypeConverter::class,
    DownloadInfoTypeConverter::class)
abstract class AppDataBase: RoomDatabase() {

    abstract fun downloadsDao(database: AppDataBase): DownloadsDao
}