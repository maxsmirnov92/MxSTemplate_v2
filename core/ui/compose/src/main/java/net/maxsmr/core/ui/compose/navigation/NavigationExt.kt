package net.maxsmr.core.ui.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import dagger.hilt.android.lifecycle.HiltViewModel
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.compose.components.ComposableDependencies
import net.maxsmr.core.ui.compose.components.IComposableViewModelsContainer
import net.maxsmr.core.ui.compose.components.LocalViewModelStoreOwner
import net.maxsmr.core.ui.compose.navigation.ViewModelResult.Companion.createWithRegister

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
 * [sameComposableHierarchy] true, если экран для [route] находится в той же иерархии,
 * что и текущий Composable-вызов
 * @return существующая или новая [VM], помеченная аннотацией [HiltViewModel];
 * Аргументы при создании не предполагаются.
 */
@Composable
inline fun <reified VM : BaseViewModel> hiltViewModel(
    route: String,
    viewModelContainer: IComposableViewModelsContainer,
    dependencies: ComposableDependencies,
    sameComposableHierarchy: Boolean = true,
    viewModelStoreOwner: ViewModelStoreOwner = LocalContext.current as ViewModelStoreOwner,
    key: String? = null,
): ViewModelResult<VM> {
    androidx.hilt.navigation.compose.hiltViewModel<VM>(
        viewModelStoreOwner = viewModelStoreOwner,
        key = key
    ).also {
        return it.createWithRegister(
            route,
            viewModelContainer,
            dependencies,
            !sameComposableHierarchy,
            viewModelStoreOwner,
            key,
        )
    }
}

/**
 * [sameComposableHierarchy] true, если экран для [route] находится в той же иерархии,
 * что и текущий Composable-вызов
 * @return существующая или новая [VM] (созданная с фабрикой или без),
 * возвращённая [ViewModelProvider],
 * для данного [route] с учётом аргументов или без них.
 * Также регистрирует полученную VM в [viewModelContainer]
 */
@Composable
inline fun <reified VM : BaseViewModel> viewModel(
    route: String,
    viewModelContainer: IComposableViewModelsContainer,
    dependencies: ComposableDependencies,
    sameComposableHierarchy: Boolean = true,
    args: Any? = null,
    viewModelStoreOwner: ViewModelStoreOwner = LocalContext.current as ViewModelStoreOwner,
    key: String? = viewModelContainer.getKeyForViewModel(VM::class.java, args),
    factory: ViewModelProvider.Factory? = null,
): ViewModelResult<VM> {
    androidx.lifecycle.viewmodel.compose.viewModel<VM>(
        viewModelStoreOwner = viewModelStoreOwner,
        key = key,
        factory = factory
    ).also {
        return it.createWithRegister(
            route,
            viewModelContainer,
            dependencies,
            !sameComposableHierarchy,
            viewModelStoreOwner,
            key,
        )
    }
}

@Composable
fun LocalViewModelStoreOwner.ClearOnDispose(
    route: String,
    viewModel: BaseViewModel,
    viewModelContainer: IComposableViewModelsContainer,
) {
    DisposableEffect(Unit) {
        onDispose {
            clear()
            viewModelContainer.unregisterViewModelWithRoute(
                route, viewModel
            )
        }
    }
}

class ViewModelResult<VM : BaseViewModel>(
    val viewModel: VM,
    val registerer: DeferredComponentRegister?,
) {

    fun interface DeferredComponentRegister {

        @Composable
        fun OnScreenCreated()
    }

    companion object {

        @Composable
        fun <VM : BaseViewModel> VM.createWithRegister(
            route: String,
            viewModelContainer: IComposableViewModelsContainer,
            dependencies: ComposableDependencies,
            needDeferred: Boolean,
            viewModelStoreOwner: ViewModelStoreOwner,
            key: String?,
        ): ViewModelResult<VM> {
            if (viewModelStoreOwner is LocalViewModelStoreOwner) {
                viewModelStoreOwner.ClearOnDispose(
                    route,
                    this,
                    viewModelContainer
                )
            }
            return ViewModelResult(this,
                if (needDeferred) {
                    // по факту вызова этой отдельной composable-функции требуется регистрация
                    DeferredComponentRegister {
                        viewModelContainer.registerViewModelWithRoute(route, key, this, dependencies)
                    }
                } else {
                    viewModelContainer.registerViewModelWithRoute(route, key, this, dependencies)
                    // отложенная регистрация не требуется
                    null
                })
        }
    }
}