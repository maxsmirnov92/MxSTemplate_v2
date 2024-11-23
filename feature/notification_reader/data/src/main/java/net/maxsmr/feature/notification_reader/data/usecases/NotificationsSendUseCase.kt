package net.maxsmr.feature.notification_reader.data.usecases

import kotlinx.coroutines.Dispatchers
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.coroutines.usecase.UseCase
import net.maxsmr.core.database.model.notification_reader.NotificationReaderEntity
import net.maxsmr.core.network.api.BaseNotificationReaderDataSource
import net.maxsmr.core.network.api.notification_reader.NotificationReaderDataRequest
import net.maxsmr.core.network.exceptions.NoPreferableConnectivityException.PreferableType
import net.maxsmr.feature.download.data.manager.checkPreferableConnection
import net.maxsmr.feature.notification_reader.data.NotificationReaderRepository
import javax.inject.Inject

class NotificationsSendUseCase @Inject constructor(
    private val readerRepo: NotificationReaderRepository,
    private val dataSource: BaseNotificationReaderDataSource,
) : UseCase<NotificationsSendUseCase.Parameters, Unit>(Dispatchers.IO) {

    override suspend fun execute(parameters: Parameters) {
        val notifications = parameters.notifications
        if (notifications.isEmpty()) return
        logger.d("Execute sending notifications, parameters: $parameters")

        readerRepo.upsertNotifications(notifications.map {
            if (it.status != NotificationReaderEntity.Loading) {
                it.copy(status = NotificationReaderEntity.Loading)
            } else {
                it
            }
        })

        try {
            baseApplicationContext.checkPreferableConnection(parameters.preferredConnectionTypes)

            dataSource.notifyData(notifications.map {
                NotificationReaderDataRequest(
                    it.contentText,
                    it.packageName,
                    it.timestamp
                )
            })
            val ids = notifications.map { it.id }
            logger.d("Notifications were sent successfully, ids: $ids")
            readerRepo.removeNotificationsByIds(ids)
            logger.d("ids after delete: ${readerRepo.getNotificationsRaw().map { it.id }}")

        } catch (e: Exception) {
            readerRepo.upsertNotifications(notifications.map {
                it.copy(status = NotificationReaderEntity.Failed(e))
            })
            throw e
        }
    }

    data class Parameters(
        val notifications: List<NotificationReaderEntity>,
        val preferredConnectionTypes: Set<PreferableType> = setOf()
    )
}
