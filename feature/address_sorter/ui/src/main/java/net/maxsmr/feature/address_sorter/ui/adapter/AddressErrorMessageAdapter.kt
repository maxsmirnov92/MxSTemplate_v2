package net.maxsmr.feature.address_sorter.ui.adapter

import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDelegationAdapter

class AddressErrorMessageAdapter(listener: AddressErrorMessageListener): BaseDelegationAdapter<AddressErrorMessageData>(
    addressErrorMessageAdapterDelegate(listener)
)