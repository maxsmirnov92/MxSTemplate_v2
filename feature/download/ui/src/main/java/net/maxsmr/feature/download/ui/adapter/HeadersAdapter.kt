package net.maxsmr.feature.download.ui.adapter

import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseAdapterData
import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDelegationAdapter

class HeadersAdapter(listener: HeaderListener): BaseDelegationAdapter<HeaderInfoAdapterData>(headersDelegateAdapter(listener))