package net.maxsmr.core.ui.compose.components

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import net.maxsmr.core.android.base.BaseViewModel

/**
 * Контейнер для хранения [BaseViewModel], полученных от Composable-функций,
 * регистрации экранных компонентов
 * и предоставления [ViewModelProvider.Factory] для данного типа VM и аргументов по требованию
 */
interface IComposableViewModelsContainer {

    fun <VM : BaseViewModel> getFactoryForViewModel(
        viewModelClass: Class<VM>,
        args: IFactoryArgs<VM>?,
    ): ViewModelProvider.Factory?

    fun <VM : BaseViewModel> getViewModelForRoute(route: String): VM?

    /**
     * @return true, если компонент был создан
     */
    @Composable
    fun registerViewModelWithRoute(
        route: String,
        key: String?,
        viewModel: BaseViewModel,
        dependencies: ComposableDependencies,
    ): Boolean

    fun unregisterViewModelWithRoute(
        route: String,
        viewModel: BaseViewModel,
    )

    fun <VM : BaseViewModel> getKeyForViewModel(
        clazz: Class<VM>,
        args: Any? = null
    ): String? = if (args != null) {
        clazz.name + ":$args"
    } else {
        null
    }

    interface IFactoryArgs<VM : BaseViewModel>
}