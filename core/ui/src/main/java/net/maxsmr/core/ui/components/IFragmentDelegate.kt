package net.maxsmr.core.ui.components

import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.components.fragments.BaseVmFragment

interface IFragmentDelegate {

    val fragment: BaseVmFragment<*>

    val viewModel: BaseViewModel

    fun onViewCreated(delegate: AlertFragmentDelegate<*>) {}

    fun onResume() {}

    fun onViewDestroyed() {}
}