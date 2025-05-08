package net.maxsmr.core.ui.components.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.CallSuper
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.alert.representation.AlertRepresentation


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

abstract class BaseNavigationFragment<VM : BaseViewModel, AR: AlertRepresentation> : BaseMenuFragment<VM, AR>(),
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
        findNavController().currentBackStackEntry?.savedStateHandle?.get<T>(key)

    fun <T> getNavigationResultLiveData(key: String = KEY_FRAGMENT_RESULT) =
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<T>(key)

    fun <T> setNavigationResult(result: T, key: String = KEY_FRAGMENT_RESULT) {
        findNavController().previousBackStackEntry?.savedStateHandle?.set(key, result)
    }

    /**
     * Возможна ли навигация с этого фрагмента в текущем состоянии
     * @param navigationAction целевое действие навигации; можно вызвать отложенно по готовности
     */
    open fun canNavigate(isFromBackPressed: Boolean, navigationAction: () -> Unit) = true

    /**
     * @return true, если нажатие на этом фрагменте было обработано
     */
    open fun onUpPressed() = false

    protected open fun onBackPressed(): Boolean {
        navigationActor.navigateUp()
        return true
    }

    companion object {

        private const val KEY_FRAGMENT_RESULT = "result"

    }
}