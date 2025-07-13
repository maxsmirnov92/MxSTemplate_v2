package net.maxsmr.core.ui.compose.alert.representation

import androidx.compose.runtime.Composable
import net.maxsmr.core.ui.alert.representation.AlertRepresentation

/**
 * Composable-отображение алерта посредством [showAlertAction]
 */
class ComposableAlertRepresentation(
    private val showAlertAction: @Composable () -> Unit
) : AlertRepresentation {

    @Composable
    fun Show() {
        showAlertAction()
    }
}