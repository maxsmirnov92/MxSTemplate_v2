package net.maxsmr.core.ui.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import dagger.hilt.android.lifecycle.HiltViewModel
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.compose.components.ComposableDependencies
import net.maxsmr.core.ui.compose.components.IComposableViewModelsContainer

fun getRouteWithArgs(
    baseRoute: String,
    vararg args: String,
): String {
    val result = StringBuilder(baseRoute)
    args.forEach { arg ->
        arg.takeIf { it.isNotEmpty() }?.let {
            result.append("/$it")
        }
    }
    return result.toString()
}

/**
 * @return существующая или новая [VM], помеченная аннотацией [HiltViewModel];
 * Аргументы при создании не предполагаются.
 */
@Composable
inline fun <reified VM : BaseViewModel> hiltViewModel(
    route: String,
    viewModelContainer: IComposableViewModelsContainer,
    dependencies: ComposableDependencies,
    key: String? = null,
): VM {
    return androidx.hilt.navigation.compose.hiltViewModel<VM>(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner,
        key = key
    ).also {
        viewModelContainer.registerViewModelByRoute(route, it, dependencies)
    }
}

/**
 * @return существующая или новая [VM] (созданная с фабрикой или без),
 * возвращённая [ViewModelProvider],
 * для данного [route] с учётом аргументов или без них.
 * С активити ли фрагментов в кач-ве [ViewModelStoreOwner].
 * Также регистрирует полученную VM в [viewModelContainer]
 */
@Composable
inline fun <reified VM : BaseViewModel> viewModel(
    route: String,
    viewModelContainer: IComposableViewModelsContainer,
    dependencies: ComposableDependencies,
    args: Any? = null,
    key: String? = viewModelContainer.getKeyForViewModel(VM::class.java, args),
    factory: ViewModelProvider.Factory? = null,
): VM = androidx.lifecycle.viewmodel.compose.viewModel<VM>(
    viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner,
    key = key,
    factory = factory
).also {
    viewModelContainer.registerViewModelByRoute(route, it, dependencies)
}