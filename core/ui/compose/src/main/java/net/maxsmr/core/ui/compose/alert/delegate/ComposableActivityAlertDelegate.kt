package net.maxsmr.core.ui.compose.alert.delegate


import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_BATTERY_OPTIMIZATION
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_NO_INTERNET
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_PERMISSION_YES_NO
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_PICKER_ERROR
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_PROGRESS
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_SERVER_ERROR
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_UNKNOWN_ERROR
import net.maxsmr.core.android.base.BaseViewModel.Companion.SNACKBAR_TAG_QUEUE
import net.maxsmr.core.android.base.BaseViewModel.Companion.TOAST_TAG_QUEUE
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.ui.alert.BaseAlertDelegate
import net.maxsmr.core.ui.alert.representation.StandardAlertRepresentation
import net.maxsmr.core.ui.alert.representation.asToast
import net.maxsmr.core.ui.compose.alert.ComposableAlertHandler
import net.maxsmr.core.ui.compose.alert.representation.ComposableAlertRepresentation
import net.maxsmr.core.ui.compose.alert.representation.asOkDialog
import net.maxsmr.core.ui.compose.alert.representation.asProgressDialog
import net.maxsmr.core.ui.compose.alert.representation.asSnackbar
import net.maxsmr.core.ui.compose.alert.representation.asYesNoDialog

/**
 * [BaseAlertDelegate] с комбинированными репрезентациями:
 * на основе Compose - для диалогов;
 * стандартной - для тостов и снеков
 */
open class ComposableActivityAlertDelegate<VM : BaseViewModel>(
    protected val activity: ComponentActivity,
    protected val viewModel: VM,
    private val scope: CoroutineScope,
    private val state: SnackbarHostState,
)  {

    private val alertHandler: ComposableAlertHandler by lazy { ComposableAlertHandler(activity) }

    @Composable
    open fun HandleCommonAlertDialogs() {
        BindDefaultProgress()
        BindAlertDialog(DIALOG_TAG_NO_INTERNET) { it.asOkDialog(activity) }
        BindAlertDialog(DIALOG_TAG_SERVER_ERROR) { it.asOkDialog(activity) }
        BindAlertDialog(DIALOG_TAG_UNKNOWN_ERROR) { it.asOkDialog(activity) }
        BindAlertDialog(DIALOG_TAG_PERMISSION_YES_NO) {
            it.asYesNoDialog(activity)
        }
        BindAlertDialog(DIALOG_TAG_PICKER_ERROR) {
            it.asOkDialog(activity)
        }
        BindAlertDialog(DIALOG_TAG_BATTERY_OPTIMIZATION) {
            it.asOkDialog(activity)
        }
    }

    fun bindStandardAlert(
        dialogQueue: AlertQueue,
        tag: String,
        representationFactory: (Alert) -> StandardAlertRepresentation?,
    ) {
        alertHandler.handleStandard(dialogQueue, tag, representationFactory)
    }

    fun bindAlertSnackbar(tag: String, representationFactory: (Alert) -> StandardAlertRepresentation?) {
        bindStandardAlert(viewModel.snackbarQueue, tag, representationFactory)
    }

    fun bindAlertToast(tag: String, representationFactory: (Alert) -> StandardAlertRepresentation?) {
        bindStandardAlert(viewModel.toastQueue, tag, representationFactory)
    }

    @Composable
    fun BindComposableAlert(
        dialogQueue: AlertQueue,
        tag: String,
        representationFactory: (Alert) -> ComposableAlertRepresentation?,
    ) {
        alertHandler.HandleComposable(dialogQueue, tag, representationFactory)
    }

    @Composable
    fun BindAlertDialog(tag: String, representationFactory: (Alert) -> ComposableAlertRepresentation?) {
        BindComposableAlert(viewModel.dialogQueue, tag, representationFactory)
    }

    open fun handleSnackbarAlerts() {
        bindAlertSnackbar(SNACKBAR_TAG_QUEUE) {
            it.asSnackbar(
                activity,
                scope,
                state
            )
        }
    }

    open fun handleToastAlerts() {
        bindAlertToast(TOAST_TAG_QUEUE) {
            it.asToast(activity)
        }
    }

    @Composable
    fun BindDefaultProgress(
        tag: String = DIALOG_TAG_PROGRESS,
        cancelable: Boolean = false,
        onCancel: (() -> Unit)? = null,
    ) {
        BindComposableAlert(viewModel.dialogQueue, tag) {
            it.asProgressDialog(activity, cancelable, onCancel)
        }
    }
}