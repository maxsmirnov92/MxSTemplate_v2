package net.maxsmr.core.ui.components

import android.content.Context
import androidx.annotation.CallSuper
import androidx.lifecycle.SavedStateHandle
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.coroutines.collectEventsWithOwner
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.core.ui.views.snackbar.SnackbarActorImpl
import net.maxsmr.core.ui.views.toast.ToastActorImpl

open class BaseHandleableViewModel(state: SavedStateHandle) : BaseViewModel(state) {

    @CallSuper
    open fun handleAlerts(context: Context, delegate: AlertFragmentDelegate<*>) {
        delegate.handleCommonAlerts(context)
    }

    @CallSuper
    open fun handleEvents(fragment: BaseVmFragment<*>) {
        if (fragment is BaseNavigationFragment<*>) {
            val navigationActor = BaseNavigationFragment.NavigationActorImpl(fragment)
            navigationCommands.collectEventsWithOwner(fragment.viewLifecycleOwner) {
                it.doAction(navigationActor)
            }
        }
        val toastActor = ToastActorImpl(fragment.requireContext())
        toastCommands.collectEventsWithOwner(fragment.viewLifecycleOwner) {
            it.doAction(toastActor)
        }
        val snackbarActor = SnackbarActorImpl(fragment.requireView())
        snackbarCommands.collectEventsWithOwner(fragment.viewLifecycleOwner) {
            it.doAction(snackbarActor)
        }
    }
}