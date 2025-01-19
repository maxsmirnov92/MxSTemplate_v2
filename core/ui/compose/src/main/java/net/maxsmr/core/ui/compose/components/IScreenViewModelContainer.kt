package net.maxsmr.core.ui.compose.components

import androidx.lifecycle.ViewModelProvider
import net.maxsmr.core.android.base.BaseViewModel

/**
 * Контейнер для хранения [BaseViewModel], полученных от Composable-функций,
 * регистрации экранных компонентов
 * и предоставления [ViewModelProvider.Factory] для данного типа VM и аргументов по требованию
 */
interface IScreenViewModelContainer {

    fun <VM: BaseViewModel> getFactoryForViewModel(viewModelClass: Class<VM>, args: IFactoryArgs<VM>?): ViewModelProvider.Factory?

    fun <VM: BaseViewModel> getViewModelForRoute(route: String): VM?

    fun onViewModelRetrieved(route: String, viewModel: BaseViewModel)

    interface IFactoryArgs<VM: BaseViewModel>
}