package net.maxsmr.feature.notification_reader.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.database.dao.notification_reader.NotificationReaderDao
import net.maxsmr.core.database.model.notification_reader.NotificationReaderEntity
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationReaderRepository @Inject constructor(
    private val dao: NotificationReaderDao,
    private val settingsRepo: SettingsDataStoreRepository,
    private val cacheRepo: CacheDataStoreRepository,
) {

    fun getNotifications(
        sortAsc: Boolean = true,
        filterFunc: NotificationReaderEntity.() -> Boolean = { true },
    ): Flow<List<NotificationReaderEntity>> {
        return if (sortAsc) {
            dao.getAll()
        } else {
            dao.getAllDesc()
        }.map { it.filter(filterFunc) }

    }

    suspend fun getNotificationsRaw(
        sortAsc: Boolean = true,
        filterFunc: suspend NotificationReaderEntity.() -> Boolean = { true },
    ): List<NotificationReaderEntity> {
        return if (sortAsc) {
            dao.getAllRaw()
        } else {
            dao.getAllDescRaw()
        }.filter { filterFunc.invoke(it) }
    }

    suspend fun insertNewNotification(
        content: String,
        packageName: String,
        timestamp: Long,
    ) {
        if (cacheRepo.isPackageInList(
                    baseApplicationContext,
                    packageName,
                    settingsRepo.getSettings().isWhitePackageList
                )
        ) {
            dao.upsert(
                NotificationReaderEntity(
                    contentText = content,
                    packageName = packageName,
                    timestamp = timestamp.takeIf { it > 0 } ?: System.currentTimeMillis(),
                    status = NotificationReaderEntity.New
                )
            )
        }
    }

    suspend fun upsertNotifications(notifications: List<NotificationReaderEntity>) {
        dao.upsert(notifications)
    }

    suspend fun removeNotifications(notifications: List<NotificationReaderEntity>) {
        removeNotificationsByIds(notifications.map { it.id })
    }

    suspend fun removeNotificationsByIds(ids: List<Long>) {
        dao.removeByIds(ids)
    }

    suspend fun updateNotificationsWithSuccess(ids: List<Long>, timestamp: Long) {
        dao.update(ids, NotificationReaderEntity.Success(timestamp))
    }
}