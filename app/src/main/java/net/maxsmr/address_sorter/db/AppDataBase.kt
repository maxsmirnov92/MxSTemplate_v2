package net.maxsmr.address_sorter.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.maxsmr.address_sorter.db.converters.DownloadInfoTypeConverter
import net.maxsmr.core.database.dao.address_sorter.AddressDao
import net.maxsmr.core.database.dao.download.DownloadsDao
import net.maxsmr.core.database.model.PrimitiveTypeConverter
import net.maxsmr.core.database.model.address_sorter.AddressEntity
import net.maxsmr.core.database.model.download.DownloadInfo

@Database(
    entities = [DownloadInfo::class, AddressEntity::class],
    version = 1
)
@TypeConverters(PrimitiveTypeConverter::class,
    DownloadInfoTypeConverter::class)
abstract class AppDataBase: RoomDatabase() {

    abstract fun downloadsDao(database: AppDataBase): DownloadsDao

    abstract fun addressDao(database: AppDataBase): AddressDao
}