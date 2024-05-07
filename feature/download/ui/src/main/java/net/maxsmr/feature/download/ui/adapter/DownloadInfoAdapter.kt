package net.maxsmr.feature.download.ui.adapter

import net.maxsmr.android.recyclerview.adapters.base.delegation.BaseDelegationAdapter

class DownloadInfoAdapter(listener: DownloadListener) : BaseDelegationAdapter<DownloadInfoAdapterData>(downloadInfoDelegateAdapter(listener))