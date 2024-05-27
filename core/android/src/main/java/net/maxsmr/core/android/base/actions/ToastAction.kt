package net.maxsmr.core.android.base.actions

import android.content.Context
import android.view.View
import android.widget.Toast
import net.maxsmr.commonutils.gui.message.TextMessage

/**
 * Действие для показа тоста
 */
data class ToastAction(
    val message: TextMessage,
    val gravity: Int? = null,
    val xOffset: Int = 0,
    val yOffset: Int = 0,
    val horizontalMargin: Float? = null,
    val verticalMargin: Float? = null,
    val duration: ToastDuration = ToastDuration.SHORT,
) : BaseViewModelAction<ToastAction.IToastActor>() {

    override fun doAction(actor: IToastActor) {
        val toast: Toast = actor.createToast(message, duration)
        gravity?.let {
            toast.setGravity(gravity, xOffset, yOffset)
        }
        if (horizontalMargin != null && verticalMargin != null) {
            toast.setMargin(horizontalMargin, verticalMargin)
        }
        toast.show()
    }

    enum class ToastDuration(val value: Int) {
        SHORT(Toast.LENGTH_SHORT),
        LONG(Toast.LENGTH_LONG)
    }

    interface IToastActor {

        fun createToast(message: TextMessage, duration: ToastDuration): Toast
    }
}