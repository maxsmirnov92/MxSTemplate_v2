package net.maxsmr.core.ui.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.databinding.ItemSuggestDropdownWhiteBinding

open class SuggestAdapter(
    context: Context,
    @LayoutRes resourceResId: Int,
    @IdRes textViewResourceId: Int,
    val onItemSelect: (Int) -> Unit,
    val onPerformFiltering: ((constraint: CharSequence?) -> Unit)? = null,
) : ArrayAdapter<String>(
    context,
    resourceResId,
    textViewResourceId
) {

    private var state: LoadState<List<String>>? = null

    fun setData(state: LoadState<List<String>>) {
        this.state = state
        if (state.isLoading || state.hasData() || state.isError()) {
            notifyDataSetChanged()
        } else {
            notifyDataSetInvalidated()
        }
    }

    fun clearData() {
        this.state = null
        notifyDataSetInvalidated()
    }

    override fun getCount(): Int {
        val state = state ?: return 0
        return if (state.isLoading || state.isError()) 1 else state.data?.size ?: 0
    }

    override fun getItem(position: Int): String {
        val state = state ?: return EMPTY_STRING
        return when {
            state.isLoading -> {
                context.getString(net.maxsmr.core.android.R.string.loading)
            }
            state.isSuccessWithData() -> {
                state.data?.getOrNull(position).orEmpty()
            }
            else -> {
                state.error?.message?.takeIf { mes -> mes.isNotEmpty() }?.let { mes ->
                    context.getString(net.maxsmr.core.android.R.string.error_format, mes)
                } ?: context.getString(net.maxsmr.core.android.R.string.error_unexpected)
            }
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return super.getView(position, convertView, parent).also {
            val state = state ?: return@also
            with(ItemSuggestDropdownWhiteBinding.bind(it)) {
                pbSuggest.isVisible = state.isLoading
                tvItemSuggest.setTextColor(ContextCompat.getColor(
                        context, if (state.isError()) {
                            R.color.textColorError
                        } else {
                            R.color.textColorPrimary
                        }
                    ))
                root.setOnClickListener {
                    onItemSelect(position)
                }
                root.isEnabled = state.isSuccessWithData()
            }
        }
    }

    override fun getFilter(): Filter {
        // уберёт промелькивание ListPopupWindow на каждое выставление результатов
        return object : Filter() {

            override fun performFiltering(constraint: CharSequence?) = FilterResults().apply {
                onPerformFiltering?.invoke(constraint)
                values = state
                count = getCount()
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                if (results.count > 0) {
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }
}