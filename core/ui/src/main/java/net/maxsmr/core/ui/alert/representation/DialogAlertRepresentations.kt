package net.maxsmr.core.ui.alert.representation

import android.content.Context
import android.content.DialogInterface
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.alert.dialog.CommonWrapBottomSheetDialog
import net.maxsmr.core.ui.alert.dialog.ProgressDialog

//Файл содержит различные варианты отображения [Alert] в UI.

@JvmOverloads
fun Alert.asOkDialog(
    context: Context,
    cancelable: Boolean = true,
    onCancel: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
): DialogRepresentation {
    check(title != null || message != null) {
        "Alert must contain title or message for being displayed as ok dialog"
    }
    check(answers.size == 1) {
        "Alert must contain exactly 1 answer for being displayed as ok dialog"
    }
    return DialogRepresentation.Builder(context, this)
        .setCancelable(cancelable)
        .setOnCancelListener { onCancel?.invoke() }
        .setPositiveButton(answers[0]) { onClick?.invoke() }
        .build()
}

@JvmOverloads
fun Alert.asMultiChoiceDialog(
    context: Context,
    cancelable: Boolean = true,
    onCancel: (() -> Unit)? = null,
    onClick: ((index: Int) -> Unit)? = null,
): DialogRepresentation {
    check(title != null || message != null) {
        "Alert must contain title or message for being displayed as multi choice dialog"
    }
    check(answers.size > 1) {
        "Alert must contain more then 1 answer for being displayed as multi choice dialog"
    }
    return DialogRepresentation.Builder(context, this)
        .setCancelable(cancelable)
        .setOnCancelListener { onCancel?.invoke() }
        .setMultiChoiceAnswers(
            DialogRepresentation.Builder.MultiChoiceAnswersData(
                answers,
                R.layout.item_dialog_choice,
                R.id.tvItem,
            ), onClick
        )
        .build()
}

@JvmOverloads
fun Alert.asYesNoDialog(
    context: Context,
    cancelable: Boolean = true,
    onCancel: (() -> Unit)? = null,
    onClick: ((yes: Boolean) -> Unit)? = null,
): DialogRepresentation {
    check(title != null || message != null) {
        "Alert must contain title or message for being displayed as yes/no dialog"
    }
    check(answers.size == 2) {
        "Alert must contain exactly 2 answers for being displayed as yes/no dialog"
    }

    return DialogRepresentation.Builder(context, this)
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
): DialogRepresentation {
    check(title != null || message != null) {
        "Alert must contain title or message for being displayed as yes/no/neutral dialog"
    }
    check(answers.size == 3) {
        "Alert must contain exactly 3 answers for being displayed as yes/no/neutral dialog"
    }
    return DialogRepresentation.Builder(context, this)
        .setCancelable(cancelable)
        .setOnCancelListener { onCancel?.invoke() }
        .setPositiveButton(answers[0]) { onClick?.invoke(DialogInterface.BUTTON_POSITIVE) }
        .setNegativeButton(answers[1]) { onClick?.invoke(DialogInterface.BUTTON_NEGATIVE) }
        .setNeutralButton(answers[2]) { onClick?.invoke(DialogInterface.BUTTON_NEUTRAL) }
        .build()
}

@JvmOverloads
fun Alert.asProgressDialog(
    context: Context,
    cancelable: Boolean,
    dimBackground: Boolean = !cancelable,
    onCancel: (() -> Unit)? = null,
): DialogRepresentation {
    check(answers.isEmpty()) {
        "Alert must contain no answers for being displayed as progress dialog"
    }
    return ProgressDialog(context, this, cancelable, dimBackground, onCancel).toRepresentation()
}

fun Alert.asCommonWrapBottomSheetDialog(
    context: Context,
    cancelable: Boolean = true,
): DialogRepresentation {
    return CommonWrapBottomSheetDialog(context, this, cancelable).toRepresentation()
}