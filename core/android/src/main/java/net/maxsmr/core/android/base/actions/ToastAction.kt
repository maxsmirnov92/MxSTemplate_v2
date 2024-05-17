package net.maxsmr.core.android.base.actions

import android.content.Context
import android.view.View
import android.widget.Toast
import net.maxsmr.commonutils.gui.message.TextMessage

/**
 * Действие для показа тоста
 */
data class ToastAction(
    val message: TextMessage? = null,
    val gravity: Int? = null,
    val xOffset: Int = 0,
    val yOffset: Int = 0,
    val horizontalMargin: Float? = null,
    val verticalMargin: Float? = null,
    val duration: ToastLength = ToastLength.SHORT,
    val customView: View? = null,
) : BaseViewModelAction<Context>() {

    override fun doAction(actor: Context) {
        super.doAction(actor)
        val toast: Toast
        if (customView == null) {
            val message = message?.get(actor) ?: throw IllegalStateException("message not specified")
            toast = Toast.makeText(actor, message, duration.value)
        } else {
            toast = Toast(actor)
            toast.view = customView
            toast.duration = duration.value
        }
        gravity?.let {
            toast.setGravity(gravity, xOffset, yOffset)
        }
        if (horizontalMargin != null && verticalMargin != null) {
            toast.setMargin(horizontalMargin, verticalMargin)
        }
        toast.show()
    }

    enum class ToastLength(val value: Int) {
        SHORT(Toast.LENGTH_SHORT),
        LONG(Toast.LENGTH_LONG)
    }
}