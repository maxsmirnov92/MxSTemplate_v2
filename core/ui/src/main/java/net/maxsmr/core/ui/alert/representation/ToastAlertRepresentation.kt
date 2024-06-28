package net.maxsmr.core.ui.alert.representation

import android.content.Context
import android.view.View
import android.widget.Toast
import net.maxsmr.core.android.base.actions.ToastExtraData
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.representation.AlertRepresentation
import net.maxsmr.core.ui.views.toast.createToast

fun Alert.asToast(
    context: Context,
    customView: View? = null,
): AlertRepresentation {
    val message = title ?: message
    val extraData = extraData as ToastExtraData?

    check(message != null) {
        "Alert must contain title or message for being displayed as toast"
    }
    check(extraData != null) {
        "Alert must contain extra data for being displayed as toast"
    }

    val toast = context.createToast(message, extraData, customView, object : Toast.Callback() {
        override fun onToastHidden() {
            super.onToastHidden()
            close()
        }
    })
    return toast.toRepresentation()
}