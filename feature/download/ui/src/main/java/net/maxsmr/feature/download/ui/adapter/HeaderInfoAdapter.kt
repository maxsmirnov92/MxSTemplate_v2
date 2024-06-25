package net.maxsmr.feature.download.ui.adapter

import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDelegationAdapter

class HeaderInfoAdapter(listener: HeaderListener): BaseDelegationAdapter<HeaderInfoAdapterData>(headersAdapterDelegate(listener))