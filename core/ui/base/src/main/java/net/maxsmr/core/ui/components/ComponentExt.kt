package net.maxsmr.core.ui.components

import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Job
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.android.base.actions.ToastAction
import net.maxsmr.core.android.coroutines.collectEventsWithOwner
import net.maxsmr.core.ui.alert.BaseAlertDelegate

fun BaseAlertDelegate<*, *>.handleAlerts() {
    handleCommonAlertDialogs()
    handleSnackbarAlerts()
    handleToastAlerts()
}

fun BaseViewModel.handleEvents(
    lifecycleOwner: LifecycleOwner,
    navigationActor: NavigationAction.INavigationActor,
    toastActor: ToastAction.IToastActor,
): List<Job> {
    return mutableListOf<Job>().apply {
        add(navigationCommands.collectEventsWithOwner(lifecycleOwner) {
            it.doAction(navigationActor)
        })
        // для совместимости с API 30 и ниже
        add(toastCommands.collectEventsWithOwner(lifecycleOwner) {
            it.doAction(toastActor)
        })
    }
}