package net.maxsmr.feature.about.adapter

import com.hannesdorfmann.adapterdelegates4.dsl.adapterDelegate
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseAdapterData
import net.maxsmr.commonutils.AppClickableSpan
import net.maxsmr.commonutils.gui.setTextWithMovementMethod
import net.maxsmr.feature.about.AboutViewModel.AboutAppDescription.DonateInfo
import net.maxsmr.feature.about.R
import net.maxsmr.feature.about.databinding.ItemDonateAddressBinding

fun donateAdapterDelegate(listener: DonateAddressClickListener) =
    adapterDelegate<DonateAddressAdapterData, BaseAdapterData>(
        R.layout.item_donate_address,
    ) {
        bind {
            with(ItemDonateAddressBinding.bind(itemView).tvDonateAddress) {
                setTextWithMovementMethod(item.address.toSpanText(AppClickableSpan(false) {
                    listener.onAddressClick(item.address)
                }))
            }
        }
    }

data class DonateAddressAdapterData(
    val address: DonateInfo.PaymentAddress,
) : BaseAdapterData {

    override fun isSame(other: BaseAdapterData): Boolean {
        return if (other !is DonateAddressAdapterData) {
            false
        } else {
            address.name.equals(other.address.name, true)
        }
    }
}

interface DonateAddressClickListener {

    fun onAddressClick(address: DonateInfo.PaymentAddress)
}