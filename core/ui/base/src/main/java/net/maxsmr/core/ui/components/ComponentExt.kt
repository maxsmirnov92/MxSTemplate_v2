package net.maxsmr.core.ui.components

import androidx.lifecycle.LifecycleOwner
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.android.base.actions.ToastAction
import net.maxsmr.core.android.coroutines.collectEventsWithOwner
import net.maxsmr.core.ui.alert.BaseAlertDelegate

fun BaseAlertDelegate<*>.handleAlerts() {
    handleCommonAlertDialogs()
    handleSnackbarAlerts()
    handleToastAlerts()
}

fun BaseViewModel.handleEvents(
    lifecycleOwner: LifecycleOwner,
    navigationActor: NavigationAction.INavigationActor,
    toastActor: ToastAction.IToastActor,
) {
    navigationCommands.collectEventsWithOwner(lifecycleOwner) {
        it.doAction(navigationActor)
    }
    // для совместимости с API 30 и ниже
    toastCommands.collectEventsWithOwner(lifecycleOwner) {
        it.doAction(toastActor)
    }
}