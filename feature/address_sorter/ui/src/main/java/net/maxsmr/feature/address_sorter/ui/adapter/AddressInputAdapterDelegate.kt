package net.maxsmr.feature.address_sorter.ui.adapter

import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.core.view.isVisible
import com.hannesdorfmann.adapterdelegates4.dsl.v2.adapterDelegate
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseAdapterData
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter
import net.maxsmr.commonutils.gui.listeners.AfterTextChangeListener
import net.maxsmr.commonutils.gui.setTextWithSelectionToEnd
import net.maxsmr.commonutils.states.ILoadState.Companion.copyOf
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.ui.adapters.SuggestAdapter
import net.maxsmr.core.ui.views.applySuggestions
import net.maxsmr.feature.address_sorter.ui.AddressSorterViewModel
import net.maxsmr.feature.address_sorter.ui.R
import net.maxsmr.feature.address_sorter.ui.databinding.ItemAddressBinding

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

            ibClear.setOnClickListener {
                listener.onClearAction(item.id)
            }

            ibNavigate.setOnClickListener {
                listener.onNavigateAction(item.item)
            }

            ibInfo.setOnClickListener {
                listener.onInfoAction(item.item)
            }

            currentTextWatcher?.let {
                etText.removeTextChangedListener(it)
            }
            val watcher = AfterTextChangeListener { s ->
                if (wasTextSetFromUser) {
                    listener.onTextChanged(item.id, s.toString())
                } else {
                    wasTextSetFromUser = true
                }
            }
            etText.addTextChangedListener(watcher)
            currentTextWatcher = watcher

            etText.init { position ->
                item.suggestsLoadState.data?.getOrNull(position)?.let { suggest ->
                    listener.onSuggestSelect(item.id, suggest)
                }
            }

            val adapter = AddressErrorMessageAdapter(object : AddressErrorMessageListener {
                override fun onClose(type: Address.ErrorType) {
                    listener.onErrorMessageClose(item.id, type)
                }
            })

            rvErrorMessages.adapter = adapter

            bind {
                item.run {
                    wasTextSetFromUser = false
                    if (!etText.setTextWithSelectionToEnd(item.address)) {
                        // изменений нет - следующий onTextChanged будет от юзера
                        wasTextSetFromUser = true
                    }
                    etText.applySuggestions(
                        suggestsLoadState.copyOf(
                            data = if (!item.isSuggested) {
                                suggestsLoadState.data?.map { it.address }.orEmpty()
                            } else {
                                listOf()
                            }
                        )
                    )
                    ibNavigate.isVisible = item.isSuggested && item.address.isNotEmpty()
                    ibInfo.isVisible = item.distance?.takeIf { it >= 0 } != null

                    rvErrorMessages.isVisible = item.exceptionsData.isNotEmpty()
                    adapter.items = item.exceptionsData
                }
            }
        }
    }


data class AddressInputData(
    val item: AddressSorterViewModel.AddressItem,
    val suggestsLoadState: LoadState<List<AddressSorterViewModel.AddressSuggestItem>>,
) : BaseAdapterData {

    val id: Long = item.id

    override fun isSame(other: BaseAdapterData): Boolean = id == (other as? AddressInputData)?.id
}

class InputViewHolder(view: View) : BaseDraggableDelegationAdapter.DragAndDropViewHolder<AddressInputData>(view) {

    internal var currentTextWatcher: TextWatcher? = null

    override val draggableView: View = itemView.findViewById(R.id.ivDrag)

    var wasTextSetFromUser = false
}

interface AddressInputListener {

    fun onTextChanged(id: Long, value: String)

    fun onSuggestSelect(id: Long, suggest: AddressSorterViewModel.AddressSuggestItem)

    fun onClearAction(id: Long)

    fun onNavigateAction(item: AddressSorterViewModel.AddressItem)

    fun onInfoAction(item: AddressSorterViewModel.AddressItem)

    fun onErrorMessageClose(id: Long, type: Address.ErrorType)
}