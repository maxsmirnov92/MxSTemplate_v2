package net.maxsmr.core.ui.content.pick.chooser.adapter

import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import net.maxsmr.commonutils.SubstringSpanInfo
import net.maxsmr.commonutils.createSpanText
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.databinding.ItemChooserPermissionsBinding

internal class IntentChooserPermissionsViewHolder(
    binding: ItemChooserPermissionsBinding,
    onClick: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private val tvTitle = binding.root

    init {
        itemView.setOnClickListener {
            onClick()
        }
    }

    fun bind(data: IntentChooserAdapterData.Permissions) {
        val res = if (data.permissions.size == 1) R.string.pick_more_permission else R.string.pick_more_permissions
        val settings = itemView.context.getString(R.string.pick_settings)
        val fullText = itemView.context.getString(res, data.permissions.joinToString { "\"$it\"" }, settings)
        tvTitle.text = fullText.createSpanText(
            SubstringSpanInfo.FirstEntry(
                settings,
                listOf(
                    UnderlineSpan(),
                    ForegroundColorSpan(ContextCompat.getColor(itemView.context, R.color.colorAccent))
                )
            )
        )
    }
}