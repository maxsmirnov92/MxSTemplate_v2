package net.maxsmr.core.ui.alert

import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.ui.alert.representation.AlertRepresentation

abstract class BaseAlertDelegate<VM : BaseViewModel, AR : AlertRepresentation>(
    protected val viewModel: VM
) {

    abstract fun handleCommonAlertDialogs()

    abstract fun handleSnackbarAlerts()

    abstract fun handleToastAlerts()

    abstract fun bindAlert(
        dialogQueue: AlertQueue,
        tag: String,
        representationFactory: (Alert) -> AR?,
    )

    /**
     * Связывает сообщения с тэгом [tag] из очереди this с конкретным способом отображения,
     * возвращаемым лямбдой [representationFactory].
     */
    fun bindAlertDialog(tag: String, representationFactory: (Alert) -> AR?) {
        bindAlert(viewModel.dialogQueue, tag, representationFactory)
    }

    fun bindAlertSnackbar(tag: String, representationFactory: (Alert) -> AR?) {
        bindAlert(viewModel.snackbarQueue, tag, representationFactory)
    }

    fun bindAlertToast(tag: String, representationFactory: (Alert) -> AR?) {
        bindAlert(viewModel.toastQueue, tag, representationFactory)
    }
}