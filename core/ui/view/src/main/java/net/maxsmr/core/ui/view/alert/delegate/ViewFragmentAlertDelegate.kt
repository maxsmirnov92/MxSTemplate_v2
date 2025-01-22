package net.maxsmr.core.ui.view.alert.delegate


import android.content.Context
import androidx.fragment.app.Fragment
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
import net.maxsmr.core.ui.view.alert.ViewAlertHandler
import net.maxsmr.core.ui.alert.BaseAlertDelegate
import net.maxsmr.core.ui.alert.representation.asToast
import net.maxsmr.core.ui.alert.representation.StandardAlertRepresentation
import net.maxsmr.core.ui.view.alert.representation.asOkDialog
import net.maxsmr.core.ui.view.alert.representation.asProgressDialog
import net.maxsmr.core.ui.view.alert.representation.asSnackbar
import net.maxsmr.core.ui.view.alert.representation.asYesNoDialog

/**
 * Делегат для fragment с функцией отображения алертов
 * с репрезентациями на основе android.app.Dialog и View;
 * 1. Использование "из коробки" при наличии прикреплённой [BaseViewModel]:
 * подразумевается первое использование не ранее onViewCreated!
 * 2. Использование по месту при подстановке [AlertQueue], диалоги с которой он должен обрабатывать;
 */
open class ViewFragmentAlertDelegate<VM : BaseViewModel>(
    val fragment: Fragment,
    viewModel: VM,
): BaseAlertDelegate<VM, StandardAlertRepresentation>(viewModel) {

    protected val context: Context by lazy {
        fragment.requireContext()
    }

    private val alertHandler: ViewAlertHandler by lazy { ViewAlertHandler(fragment) }

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

    override fun handleToastAlerts() {
        bindAlertToast(TOAST_TAG_QUEUE) {
            it.asToast(context)
        }
    }

    override fun bindAlert(
        dialogQueue: AlertQueue,
        tag: String,
        representationFactory: (Alert) -> StandardAlertRepresentation?,
    ) {
        alertHandler.handle(dialogQueue, tag, representationFactory)
    }

    /**
     * Стандартная реализация progress, нужно вызвать по месту на конкретном экране
     */
    @JvmOverloads
    fun bindDefaultProgress(
        tag: String = DIALOG_TAG_PROGRESS,
        cancelable: Boolean = false,
        onCancel: (() -> Unit)? = null,
    ) {
        bindAlert(viewModel.dialogQueue, tag) {
            it.asProgressDialog(context, cancelable, onCancel = onCancel)
        }
    }
}