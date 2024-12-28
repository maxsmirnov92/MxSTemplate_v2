package net.maxsmr.core.ui.compose.alert

import androidx.activity.ComponentActivity
import androidx.compose.material.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import net.maxsmr.core.android.base.BaseViewModel

class CombinedComposeActivityAlertDelegate<VM : BaseViewModel>(
    private val delegates: List<ComposeActivityAlertDelegate<VM>>,
    activity: ComponentActivity,
    scope: CoroutineScope,
    state: SnackbarHostState,
    viewModel: VM,
) : ComposeActivityAlertDelegate<VM>(activity, scope, state, viewModel) {

    override fun handleCommonAlertDialogs() {
        delegates.forEach {
            it.handleCommonAlertDialogs()
        }
    }

    override fun handleSnackbarAlerts() {
        delegates.forEach {
            it.handleSnackbarAlerts()
        }
    }

    override fun handleToastAlerts() {
        delegates.forEach {
            it.handleToastAlerts()
        }
    }
}