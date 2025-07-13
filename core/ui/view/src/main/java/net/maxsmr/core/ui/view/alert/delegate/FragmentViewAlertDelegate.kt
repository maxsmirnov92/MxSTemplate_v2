package net.maxsmr.core.ui.view.alert.delegate

import android.view.View
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
import net.maxsmr.core.ui.alert.delegate.BaseViewAlertDelegate
import net.maxsmr.core.ui.alert.representation.asToast
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.core.ui.view.alert.representation.asOkDialog
import net.maxsmr.core.ui.view.alert.representation.asProgressDialog
import net.maxsmr.core.ui.view.alert.representation.asSnackbar
import net.maxsmr.core.ui.view.alert.representation.asYesNoDialog

class FragmentViewAlertDelegate<VM : BaseViewModel>(
    override val fragment: BaseVmFragment<VM>,
    override val viewModel: VM,
    private val delegates: List<BaseViewAlertDelegate<VM>> = listOf(),
) : BaseFragmentViewAlertDelegate<VM>() {

    constructor(
        fragment: BaseVmFragment<VM>,
        viewModel: VM,
        vararg delegates: BaseViewAlertDelegate<VM>,
    ): this(fragment, viewModel, delegates.asList())

    override fun handleAlertDialogs() {
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
        delegates.forEach {
            it.handleAlertDialogs()
        }
    }

    override fun handleSnackbarAlerts() {
        bindAlertSnackbar(SNACKBAR_TAG_QUEUE) {
            try {
                it.asSnackbar(fragment.requireView())
            } catch (e: Exception) {
                // костыль при уже имеющемся алерте и возврате на тот же фрагмент с пересозданной view
                // при определённой вложенности с использованием Jetpack Navigation
                it.asSnackbar(fragment.requireActivity().window.decorView.findViewById(android.R.id.content))
            }
        }
    }

    override fun handleToastAlerts() {
        bindAlertToast(TOAST_TAG_QUEUE) {
            it.asToast(context)
        }
    }

    @JvmOverloads
    fun bindDefaultProgress(
        tag: String = DIALOG_TAG_PROGRESS,
        cancelable: Boolean = false,
        onCancel: (() -> Unit)? = null,
    ) {
        bindStandardAlert(viewModel.dialogQueue, tag) {
            it.asProgressDialog(context, cancelable, onCancel = onCancel)
        }
    }
}