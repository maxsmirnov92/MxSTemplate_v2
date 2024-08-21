package net.maxsmr.feature.address_sorter.ui.adapter

import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDraggableDelegationAdapter

class AddressInputAdapter(listener: AddressInputListener): BaseDraggableDelegationAdapter<AddressInputData, InputViewHolder>(
    addressInputAdapterDelegate(listener)
) {

    var isMovementEnabled: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    override fun canDismissItem(item: AddressInputData, position: Int): Boolean {
        return isMovementEnabled
    }

    override fun canDragItem(item: AddressInputData, position: Int): Boolean {
        return isMovementEnabled
    }
}