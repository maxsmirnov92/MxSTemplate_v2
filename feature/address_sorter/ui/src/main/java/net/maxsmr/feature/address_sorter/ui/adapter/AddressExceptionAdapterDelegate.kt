package net.maxsmr.feature.address_sorter.ui.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseAdapterData
import net.maxsmr.core.domain.entities.feature.address_sorter.Address.ExceptionType
import net.maxsmr.feature.address_sorter.ui.R
import net.maxsmr.feature.address_sorter.ui.databinding.ItemAddressExceptionBinding

fun addressExceptionAdapterDelegate(listener: AddressExceptionListener) =
    adapterDelegate<AddressExceptionData, AddressExceptionData>(
        R.layout.item_address_exception
    ) {
        with(ItemAddressExceptionBinding.bind(itemView)) {
            ibClose.setOnClickListener {
                listener.onClose(item.type)
            }

            bind {
                tvExceptionMessage.text = item.message?.takeIf { it.isNotEmpty() }?.let {
                    when (item.type) {
                        ExceptionType.LOCATION -> {
                            context.getString(R.string.address_sorter_error_location_format, it)
                        }

                        ExceptionType.DISTANCE -> {
                            context.getString(R.string.address_sorter_error_distance_format, it)
                        }
                    }
                } ?: run {
                    when (item.type) {
                        ExceptionType.LOCATION -> {
                            context.getString(R.string.address_sorter_error_location)
                        }

                        ExceptionType.DISTANCE -> {
                            context.getString(R.string.address_sorter_error_distance)
                        }
                    }
                }
            }
        }
    }

data class AddressExceptionData(
    val type: ExceptionType,
    val message: String?,
) : BaseAdapterData {

    override fun isSame(other: BaseAdapterData): Boolean = type == (other as? AddressExceptionData)?.type
}

interface AddressExceptionListener {

    fun onClose(type: ExceptionType)
}