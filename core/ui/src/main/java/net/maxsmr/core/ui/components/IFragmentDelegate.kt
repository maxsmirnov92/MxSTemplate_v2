package net.maxsmr.core.ui.components

import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.components.fragments.BaseVmFragment

interface IFragmentDelegate {

    fun onViewCreated(
        fragment: BaseVmFragment<*>,
        viewModel: BaseViewModel,
        delegate: AlertFragmentDelegate<*>,
    )

    fun onViewDestroyed() {}
}