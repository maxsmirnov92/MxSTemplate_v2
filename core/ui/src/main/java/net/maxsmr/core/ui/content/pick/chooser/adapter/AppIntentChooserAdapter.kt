package net.maxsmr.core.ui.content.pick.chooser.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.maxsmr.core.android.content.pick.IntentWithPermissions
import net.maxsmr.core.android.content.pick.concrete.ConcretePickerParams
import net.maxsmr.core.ui.databinding.ItemChooserAppBinding
import net.maxsmr.core.ui.databinding.ItemChooserPermissionsBinding

internal class AppIntentChooserAdapter(
    val data: List<IntentChooserAdapterData>,
    private val itemWidth: Int,
    private val onAppClick: (ConcretePickerParams, IntentWithPermissions) -> Unit,
    private val onSettingsClick: () -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_APP -> IntentChooserAppViewHolder(
                    ItemChooserAppBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                    itemWidth,
                    onAppClick
            )
            TYPE_PERMISSIONS -> IntentChooserPermissionsViewHolder(
                    ItemChooserPermissionsBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                    onSettingsClick
            )
            else -> throw IllegalArgumentException("Unexpected viewType $viewType")
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is IntentChooserAppViewHolder -> holder.bind(data[position] as IntentChooserAdapterData.App)
            is IntentChooserPermissionsViewHolder -> holder.bind(data[position] as IntentChooserAdapterData.Permissions)
        }

    }

    override fun getItemCount(): Int = data.size

    override fun getItemViewType(position: Int): Int {
        return when (data[position]) {
            is IntentChooserAdapterData.App -> TYPE_APP
            is IntentChooserAdapterData.Permissions -> TYPE_PERMISSIONS
        }
    }

    companion object {

        private const val TYPE_APP = 1
        private const val TYPE_PERMISSIONS = 2
    }
}