package net.maxsmr.core.ui.components.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.CallSuper
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.android.base.actions.NavigationAction.NavigationCommand
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.components.BaseHandleableViewModel


interface INavigationHost {

    /**
     * Вызов необходим на каждом фрагменте графа с Toolbar в разметке
     */
    fun registerToolbarWithNavigation(toolbar: Toolbar, fragment: BaseNavigationFragment<*>)
}

// TODO ?
interface INavigationDestination {

    fun onUserInteraction() {}
}

abstract class BaseNavigationFragment<VM : BaseHandleableViewModel> : BaseMenuFragment<VM>(),
        INavigationDestination {

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

    protected open fun onBackPressed(): Boolean {
        navigateUp()
        return true
    }

    protected fun navigateUp() {
        if (!findNavController().navigateUp()) { // (requireActivity() as BaseNavigationActivity).appBarConfiguration
            requireActivity().finish()
        }
    }

    class NavigationActorImpl(private val fragment: BaseNavigationFragment<*>) : NavigationAction.INavigationActor {

        override fun doNavigate(command: NavigationCommand) {
            when (command) {
                is NavigationCommand.ToDirectionWithNavDirections -> fragment.findNavController().navigate(
                    command.directions.actionId,
                    command.directions.arguments,
                    command.navOptions,
                    command.navigatorExtras,
                )
                is NavigationCommand.ToDirectionWithRoute -> fragment.findNavController().navigate(
                    command.route,
                    command.navOptions,
                    command.navigatorExtras,
                )
                is NavigationCommand.Back -> {
//                    fragment.requireActivity().onBackPressed()
                    fragment.navigateUp()
                }
                else -> {
                    throw IllegalArgumentException("Unknown command: $command")
                }
            }
        }
    }

    companion object {

        private const val KEY_FRAGMENT_RESULT = "result"

    }
}