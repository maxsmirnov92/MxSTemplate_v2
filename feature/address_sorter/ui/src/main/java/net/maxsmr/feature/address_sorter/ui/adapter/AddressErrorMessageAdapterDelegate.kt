package net.maxsmr.feature.address_sorter.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseAdapterData
import net.maxsmr.core.domain.entities.feature.address_sorter.Address.ErrorType
import net.maxsmr.feature.address_sorter.ui.R
import net.maxsmr.feature.address_sorter.ui.databinding.ItemAddressErrorMessageBinding

fun addressErrorMessageAdapterDelegate(listener: AddressErrorMessageListener) =
    adapterDelegate<AddressErrorMessageData, AddressErrorMessageData>(
        R.layout.item_address_error_message
    ) {
        with(ItemAddressErrorMessageBinding.bind(itemView)) {
            ibClose.setOnClickListener {
                listener.onClose(item.type)
            }

            bind {
                tvErrorMessage.text = item.message?.takeIf { it.isNotEmpty() }?.let {
                    when (item.type) {
                        ErrorType.LOCATION -> {
                            context.getString(R.string.address_sorter_error_location_format, it)
                        }

                        ErrorType.ROUTING -> {
                            context.getString(R.string.address_sorter_error_routing_format, it)
                        }
                    }
                } ?: run {
                    when (item.type) {
                        ErrorType.LOCATION -> {
                            context.getString(R.string.address_sorter_error_location)
                        }

                        ErrorType.ROUTING -> {
                            context.getString(R.string.address_sorter_error_routing)
                        }
                    }
                }
            }
        }
    }

data class AddressErrorMessageData(
    val type: ErrorType,
    val message: String?,
) : BaseAdapterData {

    override fun isSame(other: BaseAdapterData): Boolean = type == (other as? AddressErrorMessageData)?.type
}

interface AddressErrorMessageListener {

    fun onClose(type: ErrorType)
}