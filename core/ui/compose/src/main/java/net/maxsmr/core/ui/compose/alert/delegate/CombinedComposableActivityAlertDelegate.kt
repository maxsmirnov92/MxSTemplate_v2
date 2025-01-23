package net.maxsmr.core.ui.compose.alert.delegate

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import net.maxsmr.core.android.base.BaseViewModel

class CombinedComposableActivityAlertDelegate<VM : BaseViewModel>(
    private val delegates: List<ComposableActivityAlertDelegate<VM>>,
    activity: ComponentActivity,
    viewModel: VM,
    scope: CoroutineScope,
    state: SnackbarHostState,
) : ComposableActivityAlertDelegate<VM>(activity, viewModel, scope, state) {

    @Composable
    override fun HandleCommonAlertDialogs() {
        delegates.forEach {
            it.HandleCommonAlertDialogs()
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