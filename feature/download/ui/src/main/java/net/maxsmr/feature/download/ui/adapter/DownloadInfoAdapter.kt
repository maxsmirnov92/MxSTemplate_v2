package net.maxsmr.feature.download.ui.adapter

import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter.DragAndDropViewHolder

class DownloadInfoAdapter(listener: DownloadListener) : BaseDraggableDelegationAdapter<DownloadInfoAdapterData, DragAndDropViewHolder<DownloadInfoAdapterData>>(
    downloadInfoAdapterDelegate(listener)
) {

    override fun canDragItem(item: DownloadInfoAdapterData, position: Int): Boolean = false

    override fun canDismissItem(item: DownloadInfoAdapterData, position: Int): Boolean {
        return !item.downloadInfo.isLoading
    }
}