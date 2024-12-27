package net.maxsmr.core.ui.alert

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import net.maxsmr.commonutils.asContextOrThrow
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_PROGRESS
import net.maxsmr.core.android.base.BaseViewModel.Companion.TOAST_TAG_QUEUE
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.AlertHandler
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.android.base.alert.representation.AlertRepresentation
import net.maxsmr.core.ui.alert.representation.asToast

/**
 * Делегат для любого [LifecycleOwner] с функцией отображения алертов;
 * 1. Использование "из коробки" при наличии прикреплённой [BaseViewModel]:
 * подразумевается первое использование не ранее onViewCreated!
 * 2. Использование по месту при подстановке [AlertQueue], диалоги с которой он должен обрабатывать
 */
abstract class BaseAlertDelegate<VM: BaseViewModel>(
    protected val context: Context,
    protected val lifecycleOwner: LifecycleOwner,
    protected val viewModel: VM
) {

    protected val alertHandler: AlertHandler by lazy { AlertHandler(lifecycleOwner) }

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

    abstract fun bindDefaultProgress(
        dialogQueue: AlertQueue,
        tag: String = DIALOG_TAG_PROGRESS,
        cancelable: Boolean = false,
        onCancel: (() -> Unit)? = null,
    )

    abstract fun handleCommonAlertDialogs()

    abstract fun handleSnackbarAlerts()

    open fun handleToastAlerts() {
        val context = context
        bindAlertToast(TOAST_TAG_QUEUE) {
            it.asToast(context)
        }
    }
}