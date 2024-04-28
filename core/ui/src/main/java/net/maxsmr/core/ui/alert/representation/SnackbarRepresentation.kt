package net.maxsmr.core.ui.alert.representation

import com.google.android.material.snackbar.Snackbar
import net.maxsmr.core.android.base.alert.representation.AlertRepresentation

fun Snackbar.toRepresentation() = SnackbarRepresentation(this)

class SnackbarRepresentation(
        private val snackbar: Snackbar,
) : AlertRepresentation {

    override fun show() {
        snackbar.show()
    }

    override fun hide() {
        snackbar.dismiss()
    }
}