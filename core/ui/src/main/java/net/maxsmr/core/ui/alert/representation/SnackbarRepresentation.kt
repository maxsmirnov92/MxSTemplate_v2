package net.maxsmr.core.ui.alert.representation

import com.google.android.material.snackbar.Snackbar
import net.maxsmr.core.android.base.alert.representation.AlertRepresentation

internal fun Snackbar.toRepresentation() = SnackbarRepresentation(this)

internal class SnackbarRepresentation(
    private val snackbar: Snackbar,
) : AlertRepresentation {

    override fun show() {
        if (snackbar.isShown) return
        snackbar.show()
    }

    override fun hide() {
        if (!snackbar.isShown) return
        snackbar.dismiss()
    }
}