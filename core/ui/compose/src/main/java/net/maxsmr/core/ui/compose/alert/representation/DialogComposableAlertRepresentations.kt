package net.maxsmr.core.ui.compose.alert.representation

import android.content.Context
import android.content.DialogInterface
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.ui.compose.alert.representation.DialogComposableAlertRepresentationBuilder.MultiChoiceAnswersData.AnswerType

// Файл содержит различные варианты отображения [Alert] в Compose.

@JvmOverloads
fun Alert.asOkDialog(
    context: Context,
    cancelable: Boolean = true,
    onCancel: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
): ComposableAlertRepresentation {
    check(title != null || message != null) {
        "Alert must contain title or message for being displayed as ok dialog"
    }
    check(answers.size == 1) {
        "Alert must contain exactly 1 answer for being displayed as ok dialog"
    }
    return DialogComposableAlertRepresentationBuilder(context, this)
        .setCancelable(cancelable)
        .setOnCancelListener { onCancel?.invoke() }
        .setPositiveButton(answers[0]) { onClick?.invoke() }
        .build()
}

@JvmOverloads
fun Alert.asMultiChoiceDialog(
    context: Context,
    @StringRes confirmTextResId: Int = android.R.string.ok,
    cancelable: Boolean = true,
    type: AnswerType = AnswerType.TEXT,
    onCancel: (() -> Unit)? = null,
    onClick: ((answers: List<Alert.Answer>) -> Unit)? = null,
): ComposableAlertRepresentation {
    check(title != null || message != null) {
        "Alert must contain title or message for being displayed as multi choice dialog"
    }
    check(answers.size > 1) {
        "Alert must contain more then 1 answer for being displayed as multi choice dialog"
    }
    return DialogComposableAlertRepresentationBuilder(context, this)
        .setCancelable(cancelable)
        .setOnCancelListener { onCancel?.invoke() }
        .setMultiChoiceAnswers(
            DialogComposableAlertRepresentationBuilder.MultiChoiceAnswersData(
                answers,
                type,
            ),
            onClick
        ).apply {
            if (type == AnswerType.CHECKBOX || answers.any { !it.closeAfterSelect }) {
                setPositiveButton(
                    Alert.Answer(confirmTextResId)
                )
            }
        }
        .build()
}

@JvmOverloads
fun Alert.asYesNoDialog(
    context: Context,
    cancelable: Boolean = true,
    onCancel: (() -> Unit)? = null,
    onClick: ((yes: Boolean) -> Unit)? = null,
): ComposableAlertRepresentation {
    check(title != null || message != null) {
        "Alert must contain title or message for being displayed as yes/no dialog"
    }
    check(answers.size == 2) {
        "Alert must contain exactly 2 answers for being displayed as yes/no dialog"
    }

    return DialogComposableAlertRepresentationBuilder(context, this)
        .setCancelable(cancelable)
        .setOnCancelListener { onCancel?.invoke() }
        .setPositiveButton(answers[0]) { onClick?.invoke(true) }
        .setNegativeButton(answers[1]) { onClick?.invoke(false) }
        .build()
}

@JvmOverloads
fun Alert.asYesNoNeutralDialog(
    context: Context,
    cancelable: Boolean = true,
    onCancel: (() -> Unit)? = null,
    onClick: ((Int) -> Unit)? = null,
): ComposableAlertRepresentation {
    check(title != null || message != null) {
        "Alert must contain title or message for being displayed as yes/no/neutral dialog"
    }
    check(answers.size == 3) {
        "Alert must contain exactly 3 answers for being displayed as yes/no/neutral dialog"
    }
    return DialogComposableAlertRepresentationBuilder(context, this)
        .setCancelable(cancelable)
        .setOnCancelListener { onCancel?.invoke() }
        .setPositiveButton(answers[0]) { onClick?.invoke(DialogInterface.BUTTON_POSITIVE) }
        .setNegativeButton(answers[1]) { onClick?.invoke(DialogInterface.BUTTON_NEGATIVE) }
//        .setNeutralButton(answers[2]) { onClick?.invoke(DialogInterface.BUTTON_NEUTRAL) }
        .build()
}


@JvmOverloads
fun Alert.asProgressDialog(
    context: Context,
    cancelable: Boolean,
    onCancel: (() -> Unit)? = null,
): ComposableAlertRepresentation {
    check(answers.isEmpty()) {
        "Alert must contain no answers for being displayed as progress dialog"
    }
    val title = title?.get(context)?.toString().orEmpty()
    val message = message?.get(context)?.toString().orEmpty()
    return ComposableAlertRepresentation {
        AlertDialog(
            onDismissRequest = {
                close()
                onCancel?.invoke()
            },
            title = if (title.isNotEmpty()) {
                {
                    Text(title)
                }
            } else {
                null
            },
            text = {
                if (message.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = message,
                        )
                    }
                } else {
                    CircularProgressIndicator()
                }
            },
            confirmButton = {

            },
            properties = DialogProperties(
                dismissOnBackPress = cancelable,
                dismissOnClickOutside = cancelable
            )
        )
    }
}