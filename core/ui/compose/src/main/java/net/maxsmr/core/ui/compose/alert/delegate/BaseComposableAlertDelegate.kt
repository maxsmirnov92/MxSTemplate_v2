package net.maxsmr.core.ui.compose.alert.delegate

import androidx.compose.runtime.Composable
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.ui.alert.delegate.BaseAlertDelegate
import net.maxsmr.core.ui.compose.alert.ComposableAlertHandler
import net.maxsmr.core.ui.compose.alert.representation.ComposableAlertRepresentation

/**
 * AlertDelegate с комбинированными репрезентациями:
 * на основе Compose - для диалогов;
 * стандартной - для тостов и снеков
 */
abstract class BaseComposableAlertDelegate<VM : BaseViewModel>: BaseAlertDelegate<VM>() {

    private val composableAlertHandler: ComposableAlertHandler by lazy { ComposableAlertHandler() }

    @Composable
    abstract fun HandleAlertDialogs()

    @Composable
    fun BindAlertDialog(tag: String, representationFactory: (Alert) -> ComposableAlertRepresentation) {
        BindComposableAlert(viewModel.dialogQueue, tag, representationFactory)
    }

    @Composable
    fun BindComposableAlert(
        dialogQueue: AlertQueue,
        tag: String,
        representationFactory: (Alert) -> ComposableAlertRepresentation,
    ) {
        composableAlertHandler.Handle(dialogQueue, tag, representationFactory)
    }
}