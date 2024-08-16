package net.maxsmr.feature.address_sorter.ui.adapter

import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.hannesdorfmann.adapterdelegates4.dsl.v2.adapterDelegate
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseAdapterData
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter
import net.maxsmr.commonutils.gui.listeners.TextChangeListener
import net.maxsmr.commonutils.gui.setTextWithSelectionToEnd
import net.maxsmr.commonutils.states.ILoadState.Companion.copyOf
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.core.ui.adapters.SuggestAdapter
import net.maxsmr.core.ui.views.applySuggestions
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel
import net.maxsmr.feature.address_sorter.ui.R
import net.maxsmr.feature.address_sorter.ui.databinding.ItemAddressBinding
import java.io.Serializable

fun addressInputAdapterDelegate(listener: AddressInputListener) =
    adapterDelegate<AddressInputData, AddressInputData, InputViewHolder>(
        R.layout.item_address, createViewHolder = { InputViewHolder(it) }
    ) {

        fun AutoCompleteTextView.init(
            onItemSelect: (Int) -> Unit,
        ): ArrayAdapter<String> {
            val adapter = SuggestAdapter(
                context,
                net.maxsmr.core.ui.R.layout.item_suggest_dropdown_white,
                net.maxsmr.core.ui.R.id.tvItemSuggest,
                {
                    onItemSelect(it)
                    dismissDropDown()
                }
            )
            adapter.setDropDownViewResource(net.maxsmr.core.ui.R.layout.item_suggest_dropdown_white)
            setAdapter(adapter)
            threshold = 1
            setDropDownBackgroundResource(net.maxsmr.core.ui.R.drawable.shape_rectangle_white)
            return adapter
        }

        with(ItemAddressBinding.bind(itemView)) {

            ibRemove.setOnClickListener {
                listener.onRemove(item.id)
            }

            currentTextWatcher?.let {
                etText.removeTextChangedListener(it)
            }
            val watcher = TextChangeListener { s, _, _, _ ->
                if (!item.item.isSuggested || wasSuggestSkippedOnce) {
                    listener.onTextChanged(item.id, s.toString())
                } else if (item.item.isSuggested) {
                    wasSuggestSkippedOnce = true
                }
            }
            etText.addTextChangedListener(watcher)
            currentTextWatcher = watcher

            etText.init { position ->
                item.suggestsLoadState.data?.getOrNull(position)?.let { suggest ->
                    listener.onSuggestSelect(item.id, suggest)
                }
            }

            bind {
                item.run {
                    tilText.hint = context.getString(R.string.address_sorter_input_hint_format, item.id)
                    etText.setTextWithSelectionToEnd(item.address)
                    etText.applySuggestions(
                        suggestsLoadState.copyOf(
                            data = suggestsLoadState.data?.map { it.address }.orEmpty()
                        )
                    )
                }
            }
        }
    }



data class AddressInputData(
    val item: AddressSorterViewModel.AddressItem,
    val suggestsLoadState: LoadState<List<AddressSorterViewModel.AddressSuggestItem>>,
) : BaseAdapterData, Serializable {

    val id: Long = item.id

    override fun isSame(other: BaseAdapterData): Boolean = id == (other as? AddressInputData)?.id
}

class InputViewHolder(view: View) : BaseDraggableDelegationAdapter.DragAndDropViewHolder<AddressInputData>(view){

    internal var currentTextWatcher: TextWatcher? = null

    override val draggableView: View = itemView.findViewById(R.id.ivMore)

    var wasSuggestSkippedOnce = false
}

interface AddressInputListener {

    fun onTextChanged(id: Long, value: String)

    fun onSuggestSelect(id: Long, suggest: AddressSorterViewModel.AddressSuggestItem)

    fun onRemove(id: Long)
}