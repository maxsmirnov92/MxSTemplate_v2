package net.maxsmr.core.ui.view.alert


import androidx.fragment.app.Fragment
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_BATTERY_OPTIMIZATION
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_NO_INTERNET
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_PERMISSION_YES_NO
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_PICKER_ERROR
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_SERVER_ERROR
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_UNKNOWN_ERROR
import net.maxsmr.core.android.base.BaseViewModel.Companion.SNACKBAR_TAG_QUEUE
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.ui.alert.BaseAlertDelegate
import net.maxsmr.core.ui.view.alert.representation.asOkDialog
import net.maxsmr.core.ui.view.alert.representation.asProgressDialog
import net.maxsmr.core.ui.view.alert.representation.asSnackbar
import net.maxsmr.core.ui.view.alert.representation.asYesNoDialog

/**
 * [BaseAlertDelegate] с репрезентациями на основе android.app.Dialog и View,
 * требующий [fragment] и взаимодействующий с [viewModel]
 */
open class ViewFragmentAlertDelegate<VM : BaseViewModel>(
    protected val fragment: Fragment,
    viewModel: VM,
) : BaseAlertDelegate<VM>(fragment.requireContext(),
    fragment.viewLifecycleOwner,
    viewModel) {

    final override fun bindDefaultProgress(
        dialogQueue: AlertQueue,
        tag: String,
        cancelable: Boolean,
        onCancel: (() -> Unit)?,
    ) {
        bindAlert(dialogQueue, tag) {
            it.asProgressDialog(context, cancelable, onCancel = onCancel)
        }
    }

    override fun handleCommonAlertDialogs() {
        val context = context
        bindDefaultProgress()
        bindAlertDialog(DIALOG_TAG_NO_INTERNET) { it.asOkDialog(context) }
        bindAlertDialog(DIALOG_TAG_SERVER_ERROR) { it.asOkDialog(context) }
        bindAlertDialog(DIALOG_TAG_UNKNOWN_ERROR) { it.asOkDialog(context) }
        bindAlertDialog(DIALOG_TAG_PERMISSION_YES_NO) {
            it.asYesNoDialog(context)
        }
        bindAlertDialog(DIALOG_TAG_PICKER_ERROR) {
            it.asOkDialog(context)
        }
        bindAlertDialog(DIALOG_TAG_BATTERY_OPTIMIZATION) {
            it.asOkDialog(context)
        }
    }

    override fun handleSnackbarAlerts() {
        val view = fragment.requireView()
        bindAlertSnackbar(SNACKBAR_TAG_QUEUE) {
            it.asSnackbar(view)
        }
    }
}