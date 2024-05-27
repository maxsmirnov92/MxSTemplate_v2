package net.maxsmr.core.android.base.actions

import com.google.android.material.snackbar.Snackbar
import net.maxsmr.commonutils.gui.message.TextMessage

class SnackbarAction(
    private val message: TextMessage,
    private val length: SnackbarLength = SnackbarLength.SHORT,
): BaseViewModelAction<SnackbarAction.ISnackbarActor>() {

    override fun doAction(actor: ISnackbarActor) {
        actor.showSnackbar(message, length)
    }

    enum class SnackbarLength(val value: Int) {

        INDEFINITE(Snackbar.LENGTH_INDEFINITE),
        SHORT(Snackbar.LENGTH_SHORT),
        LONG(Snackbar.LENGTH_LONG)
    }

    interface ISnackbarActor {

        fun showSnackbar(message: TextMessage, length: SnackbarLength)
    }
}