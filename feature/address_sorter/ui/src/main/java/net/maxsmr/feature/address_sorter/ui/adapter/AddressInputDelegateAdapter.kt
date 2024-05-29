package net.maxsmr.feature.address_sorter.ui.adapter

import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.hannesdorfmann.adapterdelegates4.dsl.v2.adapterDelegate
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseAdapterData
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter.DragAndDropViewHolder.Companion.createWithDraggable
import net.maxsmr.commonutils.gui.setTextWithSelectionToEnd
import net.maxsmr.core.ui.adapters.SuggestAdapter
import net.maxsmr.core.ui.views.applySuggestions
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel
import net.maxsmr.feature.address_sorter.ui.R
import net.maxsmr.feature.address_sorter.ui.databinding.ItemAddressBinding
import java.io.Serializable

fun addressInputDelegateAdapter(listener: AddressInputListener) =
    adapterDelegate<AddressInputData, AddressInputData, BaseDraggableDelegationAdapter.DragAndDropViewHolder<AddressInputData>>(
        R.layout.item_address, createViewHolder = { it.createWithDraggable(R.id.ivMore) }
    ) {

        fun AutoCompleteTextView.init(
            onTextChanged: (CharSequence?) -> Unit,
            onItemSelect: (Int, String) -> Unit,
        ): ArrayAdapter<String> {
            val adapter = SuggestAdapter(
                context,
                net.maxsmr.core.ui.R.layout.item_suggest_dropdown_white,
                net.maxsmr.core.ui.R.id.tvItemSuggest,
                onTextChanged
            )
            adapter.setDropDownViewResource(net.maxsmr.core.ui.R.layout.item_suggest_dropdown_white)
            setAdapter(adapter)
            threshold = 1
            setDropDownBackgroundResource(net.maxsmr.core.ui.R.drawable.shape_rectangle_white)
            setOnItemClickListener { _, _, position, _ ->
                onItemSelect(position, adapter.getItem(position))
//            dismissDropDown()
            }
            return adapter
        }

//        var currentTextWatcher: TextWatcher? = null

        with(ItemAddressBinding.bind(itemView)) {

            ibRemove.setOnClickListener {
                listener.onRemove(item.id)
            }

//            currentTextWatcher?.let {
//                etText.removeTextChangedListener(it)
//            }
//            val watcher = TextChangeListener { s, start, before, count ->
//                listener.onTextChanged(item.id, s.toString())
//            }
//            etText.addTextChangedListener(watcher)
//            currentTextWatcher = watcher

            etText.init(
                {
                    listener.onTextChanged(item.id, it?.toString().orEmpty())
                },
                { position, _ ->
                    item.suggests.getOrNull(position)?.let { suggest ->
                        listener.onSuggestSelect(item.id, suggest)
                    }
                }
            )

            bind {
                item.run {
                    tilText.hint = context.getString(R.string.address_sorter_input_hint_format, item.id)
                    etText.setTextWithSelectionToEnd(item.address)
                    etText.applySuggestions(suggests.map { it.address })
                }
            }
        }
    }

data class AddressInputData(
    val item: AddressSorterViewModel.AddressItem,
    val suggests: List<AddressSorterViewModel.AddressSuggestItem>,
) : BaseAdapterData, Serializable {

    val id: Long = item.id

    override fun isSame(other: BaseAdapterData): Boolean = id == (other as? AddressInputData)?.id
}

interface AddressInputListener {

    fun onTextChanged(id: Long, value: String)

    fun onSuggestSelect(id: Long, suggest: AddressSorterViewModel.AddressSuggestItem)

    fun onRemove(id: Long)
}