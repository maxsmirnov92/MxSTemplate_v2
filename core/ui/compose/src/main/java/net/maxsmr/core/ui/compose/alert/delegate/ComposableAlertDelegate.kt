package net.maxsmr.core.ui.compose.alert.delegate

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.lifecycle.LifecycleOwner
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
import net.maxsmr.core.ui.alert.representation.asToast
import net.maxsmr.core.ui.compose.alert.representation.asOkDialog
import net.maxsmr.core.ui.compose.alert.representation.asProgressDialog
import net.maxsmr.core.ui.compose.alert.representation.asSnackbar
import net.maxsmr.core.ui.compose.alert.representation.asYesNoDialog

class ComposableAlertDelegate<VM : BaseViewModel>(
    override val lifecycleOwner: LifecycleOwner,
    override val viewModel: VM,
    private val scope: CoroutineScope,
    private val state: SnackbarHostState,
    private val delegates: List<BaseComposableAlertDelegate<VM>> = listOf()
): BaseComposableAlertDelegate<VM>() {

    @Composable
    override fun HandleAlertDialogs() {
        BindDefaultProgress()
        BindAlertDialog(DIALOG_TAG_NO_INTERNET) { it.asOkDialog(context) }
        BindAlertDialog(DIALOG_TAG_SERVER_ERROR) { it.asOkDialog(context) }
        BindAlertDialog(DIALOG_TAG_UNKNOWN_ERROR) { it.asOkDialog(context) }
        BindAlertDialog(DIALOG_TAG_PERMISSION_YES_NO) {
            it.asYesNoDialog(context)
        }
        BindAlertDialog(DIALOG_TAG_PICKER_ERROR) {
            it.asOkDialog(context)
        }
        BindAlertDialog(DIALOG_TAG_BATTERY_OPTIMIZATION) {
            it.asOkDialog(context)
        }
        delegates.forEach {
            it.HandleAlertDialogs()
        }
    }

    @Composable
    fun BindDefaultProgress(
        tag: String = DIALOG_TAG_PROGRESS,
        cancelable: Boolean = false,
        onCancel: (() -> Unit)? = null,
    ) {
        BindComposableAlert(viewModel.dialogQueue, tag) {
            it.asProgressDialog(context, cancelable, onCancel)
        }
    }

    fun handleSnackbarAlerts() {
        bindAlertSnackbar(SNACKBAR_TAG_QUEUE) {
            it.asSnackbar(
                context,
                scope,
                state
            )
        }
    }

    fun handleToastAlerts() {
        bindAlertToast(TOAST_TAG_QUEUE) {
            it.asToast(context)
        }
    }
}