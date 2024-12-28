package net.maxsmr.core.ui.compose.alert


import androidx.activity.ComponentActivity
import androidx.compose.material.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.BaseViewModel.Companion.SNACKBAR_TAG_QUEUE
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.ui.alert.BaseAlertDelegate
import net.maxsmr.core.ui.compose.alert.representation.asSnackbar

/**
 * [BaseAlertDelegate] с репрезентациями на основе Compose
 */
// TODO
open class ComposeActivityAlertDelegate<VM : BaseViewModel>(
    protected val activity: ComponentActivity,
    private val scope: CoroutineScope,
    private val state: SnackbarHostState,
    viewModel: VM,
) : BaseAlertDelegate<VM>(activity, activity, viewModel) {

    final override fun bindDefaultProgress(
        dialogQueue: AlertQueue,
        tag: String,
        cancelable: Boolean,
        onCancel: (() -> Unit)?,
    ) {

    }

    override fun handleCommonAlertDialogs() {

    }

    override fun handleSnackbarAlerts() {
        bindAlertSnackbar(SNACKBAR_TAG_QUEUE) {
            it.asSnackbar(
                activity,
                scope,
                state
            )
        }
    }
}