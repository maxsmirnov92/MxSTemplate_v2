package net.maxsmr.feature.address_sorter.ui.adapter

import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter.DragAndDropViewHolder

class AddressInputAdapter(listener: AddressInputListener): BaseDraggableDelegationAdapter<AddressInputData, DragAndDropViewHolder<AddressInputData>>(
    addressInputDelegateAdapter(listener)
)