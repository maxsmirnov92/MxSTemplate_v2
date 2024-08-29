package net.maxsmr.core.ui.alert.representation

import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import net.maxsmr.core.android.base.actions.SnackbarExtraData
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.representation.AlertRepresentation
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.views.snackbar.createSnackbar

fun Alert.asSnackbar(view: View): AlertRepresentation {
    val message = title ?: message
    val extraData = extraData as SnackbarExtraData?

    check(message != null) {
        "Alert must contain title or message for being displayed as snackbar"
    }
    check(extraData != null) {
        "Alert must contain extra data for being displayed as snackbar"
    }
    check(answers.size in 0..1) {
        "Alert may contain 0 or 1 answer for being displayed as any snackbar"
    }

    val context = view.context
    val snackbar = view.createSnackbar(message, extraData, object : Snackbar.Callback() {
        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
            super.onDismissed(transientBottomBar, event)
            if (event !in listOf(DISMISS_EVENT_MANUAL, DISMISS_EVENT_CONSECUTIVE)) {
                close()
            }
        }
    })

    answers.getOrNull(0)?.let { answer ->
        val snackbarActionTextView =
            snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_action)
        snackbarActionTextView.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            context.resources.getDimensionPixelSize(R.dimen.snackbarActionTextSize).toFloat()
        )
        snackbar.setActionTextColor(ContextCompat.getColor(context, R.color.snackbarActionColor))
        snackbar.setAction(answer.title.get(context)) {
            answer.select?.invoke()
            if (answer.closeAfterSelect) {
                snackbar.dismiss()
            }
        }
    }

    return snackbar.toRepresentation()
}