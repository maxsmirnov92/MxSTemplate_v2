package net.maxsmr.feature.notification_reader.data.usecases

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.coroutines.usecase.UseCase
import net.maxsmr.core.database.model.download.DownloadInfo.Status.Error.Companion.isCancelled
import net.maxsmr.core.database.model.notification_reader.NotificationReaderEntity
import net.maxsmr.core.network.api.BaseNotificationReaderDataSource
import net.maxsmr.core.network.api.notification_reader.NotificationReaderDataRequest
import net.maxsmr.core.network.exceptions.NoPreferableConnectivityException.PreferableType
import net.maxsmr.core.network.exceptions.OkHttpException.Companion.orNetworkCause
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

        val startTimestamp = System.currentTimeMillis()

        var isSuccess = false

        try {
            readerRepo.upsertNotifications(notifications.map {
                it.copy(status = NotificationReaderEntity.Loading(startTimestamp))
            })

            baseApplicationContext.checkPreferableConnection(parameters.preferredConnectionTypes)

            dataSource.notifyData(notifications.map {
                NotificationReaderDataRequest.NotificationReaderData(
                    it.id,
                    it.contentText,
                    it.packageName,
                    it.timestamp
                )
            })
            isSuccess = true

        } catch (e: Exception) {
            withContext(NonCancellable) {
                // здесь можно не добавлять зафейленные, которые перестали попадать в список,
                // но для индикации лучше оставить
                val failTimestamp = System.currentTimeMillis()
                readerRepo.upsertNotifications(notifications.map {
                    it.copy(
                        status = if (e.isCancelled()) {
                            NotificationReaderEntity.Cancelled(failTimestamp)
                        } else {
                            NotificationReaderEntity.Failed(failTimestamp, e.orNetworkCause())
                        }
                    )
                })
            }
            throw e
        } finally {
            if (isSuccess) {
                withContext(NonCancellable) {
                    val successTimestamp = if (!parameters.shouldRemoveIfSuccess) {
                        System.currentTimeMillis()
                    } else {
                        0
                    }
                    val ids = notifications.map { it.id }
                    logger.d("Notifications were sent successfully, ids: $ids")
                    if (parameters.shouldRemoveIfSuccess) {
                        readerRepo.removeNotificationsByIds(ids)
                        logger.d("ids after delete: ${readerRepo.getNotificationsRaw().map { it.id }}")
                    } else {
                        readerRepo.updateNotificationsWithSuccess(ids, successTimestamp)
                    }
                }
            }
        }
    }

    data class Parameters(
        val notifications: List<NotificationReaderEntity>,
        val preferredConnectionTypes: Set<PreferableType> = setOf(),
        val shouldRemoveIfSuccess: Boolean = false,
    ) {

        override fun toString(): String {
            return "Parameters(notification ids=${notifications.map { it.id }}, " +
                    "preferredConnectionTypes=$preferredConnectionTypes, " +
                    "shouldRemoveIfSuccess=$shouldRemoveIfSuccess)"
        }
    }
}
