package net.maxsmr.core.ui.alert.delegate

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import net.maxsmr.commonutils.asContextOrThrow
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.ui.alert.StandardAlertHandler
import net.maxsmr.core.ui.alert.representation.StandardAlertRepresentation

abstract class BaseAlertDelegate<VM : BaseViewModel> {

    protected abstract val viewModel: VM

    protected abstract val lifecycleOwner: LifecycleOwner

    protected val context: Context by lazy {
        lifecycleOwner.asContextOrThrow()
    }

    private val standardAlertHandler: StandardAlertHandler by lazy {
        StandardAlertHandler(lifecycleOwner)
    }

    fun bindAlertSnackbar(tag: String, representationFactory: (Alert) -> StandardAlertRepresentation) {
        bindStandardAlert(viewModel.snackbarQueue, tag, representationFactory)
    }

    fun bindAlertToast(tag: String, representationFactory: (Alert) -> StandardAlertRepresentation) {
        bindStandardAlert(viewModel.toastQueue, tag, representationFactory)
    }

    fun bindStandardAlert(
        dialogQueue: AlertQueue,
        tag: String,
        representationFactory: (Alert) -> StandardAlertRepresentation,
    ) {
        standardAlertHandler.handle(dialogQueue, tag, representationFactory)
    }
}