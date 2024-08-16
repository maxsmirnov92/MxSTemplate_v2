package net.maxsmr.feature.address_sorter.ui.adapter

import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter

class AddressInputAdapter(listener: AddressInputListener): BaseDraggableDelegationAdapter<AddressInputData, InputViewHolder>(
    addressInputAdapterDelegate(listener)
)