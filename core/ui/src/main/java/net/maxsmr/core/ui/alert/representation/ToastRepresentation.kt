package net.maxsmr.core.ui.alert.representation

import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import net.maxsmr.core.android.base.alert.representation.AlertRepresentation

internal fun Toast.toRepresentation() = ToastRepresentation(this)

internal class ToastRepresentation(
    private val toast: Toast,
) : AlertRepresentation {

    private var wasShown = false

    override fun show() {
        if (wasShown) return
        toast.show()
        wasShown = true
    }

    override fun hide() {
        if (!wasShown) return
        toast.cancel()
        wasShown = false
    }
}