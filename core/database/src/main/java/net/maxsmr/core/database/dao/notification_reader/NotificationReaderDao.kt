package net.maxsmr.core.database.dao.notification_reader

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.maxsmr.core.database.dao.UpsertDao
import net.maxsmr.core.database.model.notification_reader.NotificationReaderEntity

@Dao
abstract class NotificationReaderDao: UpsertDao<NotificationReaderEntity>() {

    @Query("select * from NotificationData")
    abstract fun getAll(): Flow<List<NotificationReaderEntity>>

    @Query("select * from NotificationData")
    abstract suspend fun getAllRaw(): List<NotificationReaderEntity>

    @Query("select * from NotificationData where id = :id")
    abstract suspend fun getById(id: Long): NotificationReaderEntity?

    @Query("delete from NotificationData where id in (:ids)")
    abstract suspend fun removeByIds(ids: List<Long>)
}