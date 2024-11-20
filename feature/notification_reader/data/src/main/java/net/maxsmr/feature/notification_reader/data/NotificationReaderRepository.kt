package net.maxsmr.feature.notification_reader.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.Companion.logException
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.database.dao.notification_reader.NotificationReaderDao
import net.maxsmr.core.database.model.notification_reader.NotificationReaderEntity
import net.maxsmr.core.network.api.BaseNotificationReaderDataSource
import net.maxsmr.core.network.api.notification_reader.NotificationReaderDataRequest
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationReaderRepository @Inject constructor(
    private val dao: NotificationReaderDao,
    private val dataSource: BaseNotificationReaderDataSource,
    private val settingsRepo: SettingsDataStoreRepository,
    private val cacheRepo: CacheDataStoreRepository,
) {

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("NotificationReaderRepo")

    fun getNotifications(filterFunc: NotificationReaderEntity.() -> Boolean = { true }): Flow<List<NotificationReaderEntity>> {
        return dao.getAll().map { it.filter(filterFunc) }
    }

    suspend fun getNotificationsRaw(filterFunc: NotificationReaderEntity.() -> Boolean = { true }): List<NotificationReaderEntity> {
        return dao.getAllRaw().filter { filterFunc.invoke(it) }
    }

    suspend fun insertNewNotification(content: String, packageName: String) {
        if (cacheRepo.isPackageInWhiteList(baseApplicationContext, packageName, settingsRepo.getSettings().isWhiteListPackages)) {
            dao.upsert(
                NotificationReaderEntity(
                    content = content,
                    packageName = packageName,
                    timestamp = System.currentTimeMillis(),
                    status = NotificationReaderEntity.New
                )
            )
        }
    }

    suspend fun upsertNotifications(notifications: List<NotificationReaderEntity>) {
        dao.upsert(notifications)
    }

    suspend fun removeNotifications(notifications: List<NotificationReaderEntity>) {
        dao.removeByIds(notifications.map { it.id })
    }

    suspend fun sendData(data: List<NotificationReaderEntity>) {
        if (data.isEmpty()) return
        logger.d("sendData, data: $data")
        upsertNotifications(data.map {
            if (it.status != NotificationReaderEntity.Loading) {
                it.copy(status = NotificationReaderEntity.Loading)
            } else {
                it
            }
        })
        try {
            dataSource.notifyData(data.map {
                NotificationReaderDataRequest(
                    it.content,
                    it.packageName,
                    it.timestamp
                )
            })
            val ids = data.map { it.id }
            logger.d("data sent successfully, removing ids $ids...")
            dao.removeByIds(ids)
            logger.d("ids after delete: ${dao.getAllRaw().map { it.id }}")
        } catch (e: Exception) {
            logException(logger, e, "notifyData")
            upsertNotifications(data.map {
                it.copy(status = NotificationReaderEntity.Failed(e))
            })
        }
    }
}