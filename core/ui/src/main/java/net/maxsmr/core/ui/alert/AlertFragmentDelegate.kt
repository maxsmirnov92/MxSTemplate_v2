package net.maxsmr.core.ui.alert

import android.content.Context
import androidx.fragment.app.Fragment
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_NO_INTERNET
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_PERMISSION_YES_NO
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_PICKER_ERROR
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_PROGRESS
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_SERVER_ERROR
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_UNKNOWN_ERROR
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.AlertHandler
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.android.base.alert.representation.AlertRepresentation
import net.maxsmr.core.ui.alert.representation.asOkDialog
import net.maxsmr.core.ui.alert.representation.asProgressDialog
import net.maxsmr.core.ui.alert.representation.asYesNoDialog

/**
 * Делегат для любого типа [Fragment] с функцией отображения алертов;
 * 1. Использование "из коробки" при наличии прикреплённой [BaseViewModel]:
 * подразумевается создание в onViewCreated!
 * 2. Использование по месту при связывании целевого фрагмента с [viewModel], диалоги с которой он должен обрабатывать
 */
class AlertFragmentDelegate<VM: BaseViewModel>(
    private val fragment: Fragment,
    private val viewModel: VM
) {

    val alertHandler: AlertHandler by lazy { AlertHandler(fragment) }

    /**
     * Связывает сообщения с тэгом [tag] из очереди this с конкретным способом отображения,
     * возвращаемым лямбдой [representationFactory].
     */
    fun bindAlertDialog(tag: String, representationFactory: (Alert) -> AlertRepresentation?) {
        bindAlertDialog(viewModel.dialogQueue, tag, representationFactory)
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
    fun bindAlertDialog(
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
        bindAlertDialog(dialogQueue, tag) {
            it.asProgressDialog(fragment.requireContext(), cancelable, onCancel = onCancel)
        }
    }

    fun handleCommonAlerts(context: Context) {
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
    }
}