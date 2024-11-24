package net.maxsmr.core.ui.alert


import android.view.View
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
import net.maxsmr.core.android.base.alert.AlertHandler
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.android.base.alert.representation.AlertRepresentation
import net.maxsmr.core.ui.alert.representation.asOkDialog
import net.maxsmr.core.ui.alert.representation.asProgressDialog
import net.maxsmr.core.ui.alert.representation.asSnackbar
import net.maxsmr.core.ui.alert.representation.asToast
import net.maxsmr.core.ui.alert.representation.asYesNoDialog
import java.lang.ref.WeakReference

/**
 * Делегат для любого типа [Fragment] с функцией отображения алертов;
 * 1. Использование "из коробки" при наличии прикреплённой [BaseViewModel]:
 * подразумевается первое использование не ранее onViewCreated!
 * 2. Использование по месту при связывании целевого фрагмента с [viewModel], диалоги с которой он должен обрабатывать
 */
class AlertFragmentDelegate<VM: BaseViewModel>(
    val fragment: Fragment,
    val viewModel: VM
) {

    val context get() = fragment.requireContext()

    val alertHandler: AlertHandler by lazy { AlertHandler(fragment) }

    /**
     * Связывает сообщения с тэгом [tag] из очереди this с конкретным способом отображения,
     * возвращаемым лямбдой [representationFactory].
     */
    fun bindAlertDialog(tag: String, representationFactory: (Alert) -> AlertRepresentation?) {
        bindAlert(viewModel.dialogQueue, tag, representationFactory)
    }

    fun bindAlertSnackbar(tag: String, representationFactory: (Alert) -> AlertRepresentation?) {
        bindAlert(viewModel.snackbarQueue, tag, representationFactory)
    }

    fun bindAlertToast(tag: String, representationFactory: (Alert) -> AlertRepresentation?) {
        bindAlert(viewModel.toastQueue, tag, representationFactory)
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
        bindDefaultProgress(viewModel.dialogQueue, tag, cancelable, onCancel)
    }

    /**
     * Упрощение функции [bind] (с захардкоженной [BaseViewModel.dialogQueue]), для показа диалогов
     */
    fun bindAlert(
        dialogQueue: AlertQueue,
        tag: String,
        representationFactory: (Alert) -> AlertRepresentation?,
    ) {
        alertHandler.handle(dialogQueue, tag, representationFactory)
    }

    /**
     * Стандартная реализация progress, нужно вызвать по месту на конкретном экране
     */
    @JvmOverloads
    fun bindDefaultProgress(
        dialogQueue: AlertQueue,
        tag: String = DIALOG_TAG_PROGRESS,
        cancelable: Boolean = false,
        onCancel: (() -> Unit)? = null,
    ) {
        bindAlert(dialogQueue, tag) {
            it.asProgressDialog(fragment.requireContext(), cancelable, onCancel = onCancel)
        }
    }

    fun handleCommonAlertDialogs() {
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

    fun handleSnackbarAlerts() {
        val view = fragment.requireView()
        bindAlertSnackbar(SNACKBAR_TAG_QUEUE) {
            it.asSnackbar(view)
        }
    }

    fun handleToastAlerts(customView: View? = null) {
        val context = context
        bindAlertToast(TOAST_TAG_QUEUE) {
            it.asToast(context, customView)
        }
    }
}