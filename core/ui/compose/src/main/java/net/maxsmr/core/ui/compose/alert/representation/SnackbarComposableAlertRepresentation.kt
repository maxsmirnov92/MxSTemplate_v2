package net.maxsmr.core.ui.compose.alert.representation

import android.content.Context
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import kotlinx.coroutines.CoroutineScope
import net.maxsmr.core.android.base.actions.SnackbarExtraData
import net.maxsmr.core.android.base.actions.SnackbarExtraData.SnackbarLength
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.ui.alert.representation.StandardAlertRepresentation

fun Alert.asSnackbar(
    context: Context,
    scope: CoroutineScope,
    state: SnackbarHostState,
): StandardAlertRepresentation {
    val textMessage = title ?: message
    val extraData = extraData as SnackbarExtraData?

    check(textMessage != null) {
        "Alert must contain title or message for being displayed as snackbar"
    }
    check(extraData != null) {
        "Alert must contain extra data for being displayed as snackbar"
    }
    check(answers.size in 0..1) {
        "Alert may contain 0 or 1 answer for being displayed as any snackbar"
    }

    val message = textMessage.get(context).toString()
    val answer: Alert.Answer? = answers.getOrNull(0)
    val action = answer?.title?.get(context)?.toString().orEmpty()

    return state.toRepresentation(
        scope,
        message,
        action,
        extraData.length.toSnackbarDuration(),
    ) {
        if (it == SnackbarResult.ActionPerformed) {
            answer?.let {
                answer.select?.invoke()
                if (answer.closeAfterSelect) {
                    state.currentSnackbarData?.dismiss()
                }
            }
        } else {
//            if (event !in listOf(DISMISS_EVENT_MANUAL, DISMISS_EVENT_CONSECUTIVE)) {
            close()
//        }
        }
    }
}

fun SnackbarLength.toSnackbarDuration() = when (this) {
    SnackbarLength.INDEFINITE -> SnackbarDuration.Indefinite
    SnackbarLength.SHORT -> SnackbarDuration.Short
    SnackbarLength.LONG -> SnackbarDuration.Long
}