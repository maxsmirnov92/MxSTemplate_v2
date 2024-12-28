package net.maxsmr.core.ui.compose.alert.representation

import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.maxsmr.core.android.base.alert.representation.AlertRepresentation

internal fun SnackbarHostState.toRepresentation(
    scope: CoroutineScope,
    message: String,
    action: String,
    duration: SnackbarDuration,
    onResult: (SnackbarResult) -> Unit,
) = SnackbarRepresentation(
    this,
    scope,
    message,
    action,
    duration,
    onResult
)

internal class SnackbarRepresentation(
    private val snackbar: SnackbarHostState,
    private val scope: CoroutineScope,
    private val message: String,
    private val action: String,
    private val duration: SnackbarDuration,
    private val onResult: (SnackbarResult) -> Unit,
) : AlertRepresentation {

    override fun show() {
        scope.launch {
            onResult(snackbar.showSnackbar(
                message = message,
                actionLabel = action,
                duration = duration
            ))
        }
    }

    override fun hide() {
        snackbar.currentSnackbarData?.dismiss()
    }
}