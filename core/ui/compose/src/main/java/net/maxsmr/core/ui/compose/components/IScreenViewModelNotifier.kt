package net.maxsmr.core.ui.compose.components

import net.maxsmr.core.android.base.BaseViewModel

interface IScreenViewModelNotifier {

    fun onViewModelRetrieved(route: String, viewModel: BaseViewModel)
}