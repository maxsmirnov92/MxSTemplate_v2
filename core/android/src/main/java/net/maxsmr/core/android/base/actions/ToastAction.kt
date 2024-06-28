package net.maxsmr.core.android.base.actions

import android.widget.Toast
import net.maxsmr.commonutils.gui.message.TextMessage
import java.io.Serializable

/**
 * Действие для показа тоста
 */
@Deprecated("use toastQueue")
data class ToastAction(
    private val message: TextMessage,
    private val data: ToastExtraData,
) : BaseViewModelAction<ToastAction.IToastActor>() {

    override fun doAction(actor: IToastActor) {
        actor.showToast(message, data)
    }

    interface IToastActor {

        fun showToast(message: TextMessage, data: ToastExtraData)
    }
}

enum class ToastDuration(val value: Int) {
    SHORT(Toast.LENGTH_SHORT),
    LONG(Toast.LENGTH_LONG)
}

data class ToastExtraData(
    val gravity: Int? = null,
    val xOffset: Int = 0,
    val yOffset: Int = 0,
    val horizontalMargin: Float? = null,
    val verticalMargin: Float? = null,
    val duration: ToastDuration = ToastDuration.SHORT,
) : Serializable