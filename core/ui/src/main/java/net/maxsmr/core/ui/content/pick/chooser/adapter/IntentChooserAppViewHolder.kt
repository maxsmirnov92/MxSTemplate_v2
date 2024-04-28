package net.maxsmr.core.ui.content.pick.chooser.adapter

import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import net.maxsmr.core.android.content.pick.IntentWithPermissions
import net.maxsmr.core.android.content.pick.concrete.ConcretePickerParams
import net.maxsmr.core.ui.databinding.ItemChooserAppBinding

internal class IntentChooserAppViewHolder(
    binding: ItemChooserAppBinding,
    itemWidth: Int,
    private val onClick: (ConcretePickerParams, IntentWithPermissions) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    private val ivIcon = binding.ivIcon
    private val tvName = binding.tvName

    init {
        itemView.updateLayoutParams<ViewGroup.LayoutParams> {
            width = itemWidth
        }
    }

    fun bind(data: IntentChooserAdapterData.App) {
        ivIcon.setImageDrawable(data.icon)
        tvName.text = data.label
        itemView.setOnClickListener {
            onClick(data.params, data.intentWithPermissions)
        }
    }
}