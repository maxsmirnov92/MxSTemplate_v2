package net.maxsmr.core.ui.views.toast

import android.content.Context
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.actions.ToastAction
import net.maxsmr.core.android.base.actions.ToastExtraData

@Deprecated("use toastQueue")
class ToastActorImpl(
    private val context: Context,
): ToastAction.IToastActor {

    override fun showToast(message: TextMessage, data: ToastExtraData) {
        context.showToast(message, data)
    }
}