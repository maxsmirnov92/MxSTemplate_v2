package net.maxsmr.core.ui.adapters

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes

open class SuggestAdapter(
    context: Context,
    @LayoutRes resourceResId: Int,
    @IdRes textViewResourceId: Int,
    val onPerformFiltering: (constraint: CharSequence?) -> Unit,
) : ArrayAdapter<String>(
    context,
    resourceResId,
    textViewResourceId
) {

    private val data = mutableListOf<String>()

    fun setData(data: List<String>) {
        this.data.clear()
        this.data.addAll(data)
        if (this.data.isNotEmpty()) {
            notifyDataSetChanged()
        } else {
            notifyDataSetInvalidated()
        }
    }

    override fun getCount(): Int = data.size

    override fun getItem(position: Int): String = data[position]

    override fun getFilter(): Filter {
        // уберёт промелькивание ListPopupWindow на каждое выставление результатов
        return object : Filter() {

            override fun performFiltering(constraint: CharSequence?) = FilterResults().apply {
                onPerformFiltering(constraint)
                values = data
                count = data.size
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