@file:JvmName("SnackbarViewAlertRepresentationKt")

package net.maxsmr.core.ui.view.alert.representation

import com.google.android.material.snackbar.Snackbar
import net.maxsmr.core.ui.alert.representation.StandardAlertRepresentation

internal fun Snackbar.toRepresentation() = SnackbarViewAlertRepresentation(this)

internal class SnackbarViewAlertRepresentation(
    private val snackbar: Snackbar,
) : StandardAlertRepresentation {

    override fun show() {
        if (snackbar.isShown) return
        snackbar.show()
    }

    override fun hide() {
        if (!snackbar.isShown) return
        snackbar.dismiss()
    }
}