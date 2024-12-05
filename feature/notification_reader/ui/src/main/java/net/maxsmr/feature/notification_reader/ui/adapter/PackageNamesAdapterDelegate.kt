package net.maxsmr.feature.notification_reader.ui.adapter

import android.widget.TextView
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseAdapterData
import net.maxsmr.commonutils.gui.setTextOrGone

fun packageNameAdapterDelegate() = adapterDelegate<PackageNameAdapterData, PackageNameAdapterData>(
    android.R.layout.simple_list_item_1
) {

    bind {
        itemView.findViewById<TextView>(android.R.id.text1)
            ?.let {
                it.setTextOrGone(item.name)
                it.minHeight = 0
                it.minimumHeight = 0
            }
    }
}

data class PackageNameAdapterData(
    val name: String,
) : BaseAdapterData {

    override fun isSame(other: BaseAdapterData): Boolean =
        name == (other as? PackageNameAdapterData)?.name
}