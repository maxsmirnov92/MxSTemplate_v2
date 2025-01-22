package net.maxsmr.core.ui.compose.alert.representation

import androidx.compose.runtime.Composable
import net.maxsmr.core.ui.alert.representation.AlertRepresentation

/**
 * Composable-отображение алерта посредством [showAlertFunc]
 */
class ComposableAlertRepresentation(
    private val showAlertFunc: @Composable () -> Unit
) : AlertRepresentation {

    @Composable
    fun Show() {
        showAlertFunc()
    }
}