package net.maxsmr.feature.notification_reader.ui.adapter

import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseAdapterData
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter.DragAndDropViewHolder.Companion.createWithDraggable
import net.maxsmr.commonutils.format.formatDate
import net.maxsmr.commonutils.gui.setTextOrGone
import net.maxsmr.core.database.model.notification_reader.NotificationReaderEntity
import net.maxsmr.core.database.model.notification_reader.NotificationReaderEntity.Status
import net.maxsmr.feature.notification_reader.ui.R
import net.maxsmr.feature.notification_reader.ui.adapter.NotificationsAdapterData.Companion.NOTIFICATION_DATETIME_FORMAT
import net.maxsmr.feature.notification_reader.ui.databinding.ItemNotificationBinding
import java.io.Serializable
import java.util.Date

fun notificationsAdapterDelegate() = com.hannesdorfmann.adapterdelegates4.dsl.v2.adapterDelegate<
        NotificationsAdapterData, NotificationsAdapterData, BaseDraggableDelegationAdapter.DragAndDropViewHolder<NotificationsAdapterData>
        >(
    R.layout.item_notification, createViewHolder = { it.createWithDraggable(R.id.containerNotification) }
) {
    val binding = ItemNotificationBinding.bind(itemView)

    with(binding) {
        bind {
            tvNotificationId.setTextOrGone(item.id.takeIf { it > 0 }?.let { "#$it" })
            tvNotificationPackageName.text = item.packageName.takeIf { it.isNotEmpty() }
                ?: getString(R.string.notification_reader_package_name_unknown)
            tvNotificationStatus.setText(item.statusTextResId)
            val status = item.status
            tvNotificationStatus.setTextColor(
                ContextCompat.getColor(
                    context, when (status) {
                        is NotificationReaderEntity.New -> {
                            R.color.textColorNotificationNew
                        }

                        is NotificationReaderEntity.Loading -> {
                            R.color.textColorNotificationLoading
                        }

                        is NotificationReaderEntity.Success -> {
                            R.color.textColorNotificationSuccess
                        }

                        is NotificationReaderEntity.Failed -> {
                            R.color.textColorNotificationFailed
                        }

                        is NotificationReaderEntity.Cancelled -> {
                            R.color.textColorNotificationCancelled
                        }
                    }
                )
            )
            if (status is NotificationReaderEntity.Failed) {
                tvNotificationFailReason.setTextOrGone(status.exception.message)
            } else {
                tvNotificationFailReason.isVisible = false
            }

            tvNotificationContentText.text =
                item.contentText.takeIf { it.isNotEmpty() } ?: getString(R.string.notification_reader_content_empty)
            tvNotificationShownTime.setTextOrGone(item.formattedTime)

            if (status is NotificationReaderEntity.BaseTimeStatus && status.timestamp > 0) {
                val textColorResId = when (status) {
                    is NotificationReaderEntity.Loading -> {
                        tvNotificationTimeName.setText(R.string.notification_reader_time_started)
                        R.color.textColorNotificationLoading
                    }

                    is NotificationReaderEntity.Success -> {
                        tvNotificationTimeName.setText(R.string.notification_reader_time_finished)
                        R.color.textColorNotificationSuccess
                    }

                    is NotificationReaderEntity.Failed -> {
                        tvNotificationTimeName.setText(R.string.notification_reader_time_finished)
                        R.color.textColorNotificationFailed
                    }

                    is NotificationReaderEntity.Cancelled -> {
                        tvNotificationTimeName.setText(R.string.notification_reader_time_cancelled)
                        R.color.textColorNotificationCancelled
                    }
                }
                tvNotificationTimeName.setTextColor(ContextCompat.getColor(context, textColorResId))
                tvNotificationTimeValue.setTextColor(ContextCompat.getColor(context, textColorResId))
                tvNotificationTimeValue.setTextOrGone(
                    formatDate(
                        Date(status.timestamp),
                        NOTIFICATION_DATETIME_FORMAT
                    )
                )
            } else {
                tvNotificationTimeValue.isVisible = false
            }
            containerNotificationTime.isVisible = tvNotificationTimeValue.isVisible
        }
    }
}

data class NotificationsAdapterData(
    val id: Long,
    val contentText: String,
    val packageName: String,
    val formattedTime: String,
    val status: Status,
) : BaseAdapterData, Serializable {

    @StringRes
    val statusTextResId = when (status) {
        is NotificationReaderEntity.New -> R.string.notification_reader_status_new
        is NotificationReaderEntity.Loading -> R.string.notification_reader_status_loading
        is NotificationReaderEntity.Success -> R.string.notification_reader_status_success
        is NotificationReaderEntity.Failed -> R.string.notification_reader_status_failed
        is NotificationReaderEntity.Cancelled -> R.string.notification_reader_status_cancelled
    }

    override fun isSame(other: BaseAdapterData): Boolean = id == (other as? NotificationsAdapterData)?.id

    companion object {

        const val NOTIFICATION_DATETIME_FORMAT: String = "dd-MM-yyyy HH:mm:ss.SSS"
    }
}