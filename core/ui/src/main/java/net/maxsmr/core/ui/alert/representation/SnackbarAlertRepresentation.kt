package net.maxsmr.core.ui.alert.representation

import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import net.maxsmr.core.android.base.actions.SnackbarAction.SnackbarLength
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.views.snackbar.createSnackbar

fun Alert.asIndefiniteSnackbar(view: View): SnackbarRepresentation {
    val message = title ?: message

    check(message != null) {
        "Alert must contain title or message for being displayed as snackbar"
    }
    check(answers.size <= 1) {
        "Alert must contain 0 or 1 answer for being displayed as snackbar"
    }

    val context = view.context
    val snackbar = view.createSnackbar(message, SnackbarLength.INDEFINITE)
    val snackbarView = snackbar.view

    answers.getOrNull(0)?.let { answer ->

        val snackbarActionTextView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_action)
        snackbarActionTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.resources.getDimensionPixelSize(R.dimen.snackbarActionTextSize).toFloat())
        snackbar.setActionTextColor(ContextCompat.getColor(context, R.color.snackbarActionColor))

        snackbar.setAction(answer.title.get(context)) {
            answer.select?.invoke()
            close()
        }
    }

    return snackbar.toRepresentation()
}