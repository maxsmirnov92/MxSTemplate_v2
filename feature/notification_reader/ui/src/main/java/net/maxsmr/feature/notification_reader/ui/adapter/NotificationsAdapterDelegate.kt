package net.maxsmr.feature.notification_reader.ui.adapter

import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseAdapterData
import net.maxsmr.commonutils.gui.setTextOrGone
import net.maxsmr.core.database.model.notification_reader.NotificationReaderEntity
import net.maxsmr.core.database.model.notification_reader.NotificationReaderEntity.Status
import net.maxsmr.feature.notification_reader.ui.R
import net.maxsmr.feature.notification_reader.ui.databinding.ItemNotificationBinding
import java.io.Serializable

fun notificationsAdapterDelegate() = adapterDelegate<NotificationsAdapterData, BaseAdapterData>(
    R.layout.item_notification
) {
    val binding = ItemNotificationBinding.bind(itemView)

    with(binding) {
        bind {
            tvNotificationId.setTextOrGone(item.id.takeIf { it > 0 }?.let { "#$it" })
            tvNotificationPackageName.text = item.packageName.takeIf { it.isNotEmpty() } ?: getString(R.string.notification_reader_package_name_unknown)
            tvNotificationStatus.setText(item.statusTextResId)
            val status = item.status
            tvNotificationStatus.setTextColor(ContextCompat.getColor(context, when(status) {
                is NotificationReaderEntity.New -> {
                    R.color.textColorNotificationNew
                }
                is NotificationReaderEntity.Loading -> {
                    R.color.textColorNotificationLoading
                }
                is NotificationReaderEntity.Failed -> {
                    net.maxsmr.core.ui.R.color.textColorError
                }
            }))
            if (status is NotificationReaderEntity.Failed) {
                tvNotificationFailReason.setTextOrGone(status.exception.message)
            } else {
                tvNotificationFailReason.isVisible = false
            }

            tvNotificationContentText.text = item.contentText.takeIf { it.isNotEmpty() } ?: getString(R.string.notification_reader_content_empty)
            tvNotificationTime.setTextOrGone(item.formattedTime)
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
    val statusTextResId = when(status) {
        is NotificationReaderEntity.New -> R.string.notification_reader_status_new
        is NotificationReaderEntity.Loading -> R.string.notification_reader_status_loading
        is NotificationReaderEntity.Failed -> R.string.notification_reader_status_failed
    }

    override fun isSame(other: BaseAdapterData): Boolean = id == (other as? NotificationsAdapterData)?.id
}