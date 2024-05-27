package net.maxsmr.core.ui.views.snackbar

import android.view.View
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.actions.SnackbarAction

class SnackbarActorImpl(private val view: View): SnackbarAction.ISnackbarActor {

    override fun showSnackbar(message: TextMessage, length: SnackbarAction.SnackbarLength) {
        view.showSnackbar(message, length)
    }
}