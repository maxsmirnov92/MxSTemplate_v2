package net.maxsmr.feature.notification_reader.ui.adapter

import android.widget.TextView
import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseAdapterData
import net.maxsmr.commonutils.gui.setTextOrGone
import net.maxsmr.core.network.api.notification_reader.AppInfo

fun appInfoAdapterDelegate() = adapterDelegate<AppInfoAdapterData, AppInfoAdapterData>(
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

data class AppInfoAdapterData(
    val name: String,
) : BaseAdapterData {

    override fun isSame(other: BaseAdapterData): Boolean =
        name == (other as? AppInfoAdapterData)?.name

    companion object {

        fun AppInfo.asAdapterData(): String {
            val result = StringBuilder()
            if (packageName.isNotEmpty()) {
                result.append(packageName)
            }
            if (appNamePrefix.isNotEmpty()) {
                if (result.isNotEmpty()) {
                    result.append(" (\"$appNamePrefix\")")
                } else {
                    result.append("\"$appNamePrefix\"")
                }
            }
            return result.toString()
        }
    }
}