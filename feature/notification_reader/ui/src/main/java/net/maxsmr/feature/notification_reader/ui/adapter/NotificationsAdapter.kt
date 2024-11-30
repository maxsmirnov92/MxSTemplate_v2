package net.maxsmr.feature.notification_reader.ui.adapter

import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter
import net.maxsmr.core.database.model.notification_reader.NotificationReaderEntity

class NotificationsAdapter: BaseDraggableDelegationAdapter<NotificationsAdapterData, BaseDraggableDelegationAdapter.DragAndDropViewHolder<NotificationsAdapterData>>(
    notificationsAdapterDelegate()
) {

    override fun canDragItem(item: NotificationsAdapterData, position: Int): Boolean = false

    override fun canDismissItem(item: NotificationsAdapterData, position: Int): Boolean = item.status is NotificationReaderEntity.Success
}