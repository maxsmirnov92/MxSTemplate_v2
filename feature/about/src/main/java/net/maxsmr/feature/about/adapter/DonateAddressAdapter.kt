package net.maxsmr.feature.about.adapter

import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseAdapterData
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDelegationAdapter

class DonateAddressAdapter(listener: DonateAddressClickListener): BaseDelegationAdapter<BaseAdapterData>(
    donateAdapterDelegate(listener)
)