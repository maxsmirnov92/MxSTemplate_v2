package net.maxsmr.core.ui.message.toast

import android.content.Context
import android.view.View
import android.widget.Toast
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.isAtLeastR
import net.maxsmr.core.android.base.actions.ToastExtraData

fun Context.showToast(
    message: TextMessage,
    data: ToastExtraData,
    callback: Toast.Callback? = null,
) {
    createToast(message, data, callback).show()
}

fun Context.createToast(
    message: TextMessage,
    data: ToastExtraData,
    callback: Toast.Callback? = null,
): Toast {
    return Toast.makeText(this, message.get(this), data.duration.value).apply {
        data.gravity?.let {
            setGravity(gravity, xOffset, yOffset)
        }
        if (data.horizontalMargin != null && data.verticalMargin != null) {
            setMargin(horizontalMargin, verticalMargin)
        }
        if (isAtLeastR()) {
            callback?.let {
                addCallback(it)
            }
        }
    }
}