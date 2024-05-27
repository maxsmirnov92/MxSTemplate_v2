package net.maxsmr.core.ui.views.toast

import android.content.Context
import android.view.View
import android.widget.Toast
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.actions.ToastAction

class ToastActorImpl(
    private val context: Context,
    private val customView: View? = null
): ToastAction.IToastActor {

    override fun createToast(message: TextMessage, duration: ToastAction.ToastDuration): Toast {
        return if (customView == null) {
            Toast.makeText(context, message.get(context), duration.value)
        } else {
            Toast(context).apply {
                view = customView
                this.duration = duration.value
            }
        }
    }
}