package net.maxsmr.core.ui.alert.representation

import android.widget.Toast

internal fun Toast.toRepresentation() = ToastAlertRepresentation(this)

internal class ToastAlertRepresentation(
    private val toast: Toast,
) : StandardAlertRepresentation {

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