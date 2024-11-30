package net.maxsmr.core.database.dao.notification_reader

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.maxsmr.core.database.dao.UpsertDao
import net.maxsmr.core.database.model.notification_reader.NotificationReaderEntity

@Dao
abstract class NotificationReaderDao: UpsertDao<NotificationReaderEntity>() {

    @Query("select * from NotificationData")
    abstract fun getAll(): Flow<List<NotificationReaderEntity>>

    @Query("select * from NotificationData order by id DESC")
    abstract fun getAllDesc(): Flow<List<NotificationReaderEntity>>

    @Query("select * from NotificationData")
    abstract suspend fun getAllRaw(): List<NotificationReaderEntity>

    @Query("select * from NotificationData order by id DESC")
    abstract suspend fun getAllDescRaw(): List<NotificationReaderEntity>

    @Query("select * from NotificationData where id = :id")
    abstract suspend fun getById(id: Long): NotificationReaderEntity?

    @Query("delete from NotificationData where id in (:ids)")
    abstract suspend fun removeByIds(ids: List<Long>)

    @Query("update NotificationData set status=:newStatus where id in (:ids)")
    abstract suspend fun update(ids: List<Long>, newStatus: NotificationReaderEntity.Status)

}