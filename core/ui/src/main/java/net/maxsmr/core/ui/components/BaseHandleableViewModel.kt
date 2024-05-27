package net.maxsmr.core.ui.components

import android.content.Context
import androidx.annotation.CallSuper
import androidx.lifecycle.SavedStateHandle
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment.Companion.handleNavigation
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.core.ui.views.snackbar.SnackbarActorImpl
import net.maxsmr.core.ui.views.toast.ToastActorImpl

open class BaseHandleableViewModel(state: SavedStateHandle): BaseViewModel(state) {

    @CallSuper
    open fun handleAlerts(context: Context, delegate: AlertFragmentDelegate<*>) {
        delegate.handleCommonAlerts(context)
    }

    @CallSuper
    open fun handleEvents(fragment: BaseVmFragment<*>) {
        if (fragment is BaseNavigationFragment<*, *>) {
            navigationCommands.observeEvents {
                fragment.handleNavigation(it)
            }
        }
        toastCommands.observeEvents(fragment.viewLifecycleOwner) { it.doAction(ToastActorImpl(fragment.requireContext())) }
        snackbarCommands.observeEvents(fragment.viewLifecycleOwner) { it.doAction(SnackbarActorImpl(fragment.requireView())) }
    }
}