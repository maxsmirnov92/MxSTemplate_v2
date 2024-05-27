package net.maxsmr.core.ui.components.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavArgs
import androidx.navigation.NavArgsLazy
import androidx.navigation.fragment.findNavController
import net.maxsmr.core.android.base.actions.NavigationCommand
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import kotlin.reflect.KClass


interface INavigationHost {

    /**
     * Вызов необходим на каждом фрагменте графа с Toolbar в разметке
     */
    fun registerToolbarWithNavigation(toolbar: Toolbar, fragment: BaseNavigationFragment<*, *>)
}

// TODO ?
interface INavigationDestination {

    fun onUserInteraction() {}
}

abstract class BaseNavigationFragment<VM : BaseHandleableViewModel, Args : NavArgs> : BaseMenuFragment<VM>(),
        INavigationDestination {

    protected abstract val argsClass: KClass<Args>?

    protected val args: Args get() = getNavArgsOrThrow()

    private var navigationHost: INavigationHost? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is INavigationHost) {
            navigationHost = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This callback will only be called when MyFragment is at least Started.
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true /* enabled by default */) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: VM) {
        super.onViewCreated(view, savedInstanceState, viewModel)
        val host = navigationHost ?: return
        view.findViewById<Toolbar>(R.id.toolbar)?.let {
            host.registerToolbarWithNavigation(it, this)
        }
    }

    override fun onDetach() {
        super.onDetach()
        navigationHost = null
    }

    fun <T> getNavigationResult(key: String = KEY_FRAGMENT_RESULT) =
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<T>(key)

    fun <T> setNavigationResult(result: T, key: String = KEY_FRAGMENT_RESULT) {
        findNavController().previousBackStackEntry?.savedStateHandle?.set(key, result)
    }

    /**
     * @return true, если нажатие на этом фрагменте было обработано
     */
    open fun onUpPressed() = false

    protected open fun onBackPressed() {
        navigateUp()
    }

    protected fun navigateUp() {
        if (!findNavController().navigateUp()) {
            requireActivity().finish()
        }
    }

    @MainThread
    protected fun getNavArgs(): Args? =
        arguments?.let { args ->
            argsClass?.let {
                NavArgsLazy(it) {
                    args
                }.value
            }
        }

    @MainThread
    private fun getNavArgsOrThrow(): Args = getNavArgs() ?: throw IllegalStateException("Navigation args are null")

    companion object {

        private const val KEY_FRAGMENT_RESULT = "result"

        @JvmStatic
        fun BaseNavigationFragment<*, *>.handleNavigation(navCommand: NavigationCommand) {
            when (navCommand) {
                is NavigationCommand.ToDirectionWithNavDirections -> findNavController().navigate(
                    navCommand.directions.actionId,
                    navCommand.directions.arguments,
                    navCommand.navOptions,
                    navCommand.navigatorExtras,
                )
                is NavigationCommand.ToDirectionWithRoute -> findNavController().navigate(
                    navCommand.route,
                    navCommand.navOptions,
                    navCommand.navigatorExtras,
                )
                is NavigationCommand.Back -> {
                    navigateUp()
                }
                else -> {
                    throw IllegalArgumentException("Unknown navCommand: $navCommand")
                }
            }
        }
    }
}