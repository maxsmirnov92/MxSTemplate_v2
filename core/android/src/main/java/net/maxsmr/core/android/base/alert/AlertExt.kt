package net.maxsmr.core.android.base.alert

import android.content.DialogInterface
import androidx.annotation.StringRes
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.R
import net.maxsmr.core.android.base.alert.queue.AlertQueueItem

@JvmOverloads
fun AlertQueueItem.Builder.showOkAlert(
    @StringRes messageResId: Int,
    @StringRes title: Int? = null,
    onConfirmClick: (() -> Unit)? = null,
) {
    showOkAlert(TextMessage(messageResId), title?.let { TextMessage(it) }, onConfirmClick)
}

/**
 * Не использовать для показа ошибок запросов. Для этого используйте [showErrorDialog]
 */
@JvmOverloads
fun AlertQueueItem.Builder.showOkAlert(
    message: TextMessage,
    title: TextMessage? = null,
    onConfirmClick: (() -> Unit)? = null,
) {
    setTitle(title)
        .setMessage(message)
        .setAnswers(Alert.Answer((android.R.string.ok)).onSelect {
            onConfirmClick?.invoke()
        })
        .build()
}

fun AlertQueueItem.Builder.showYesNoAlert(
    message: TextMessage,
    title: TextMessage? = null,
    @StringRes positiveAnswerResId: Int = R.string.yes,
    @StringRes negativeAnswerResId: Int = R.string.no,
    @StringRes neutralAnswerResId: Int? = null,
    onSelect: ((Int) -> Unit)? = null,
) {
    setTitle(title)
        .setMessage(message)
        .setAnswers(
            mutableListOf<Alert.Answer>().apply {
                add(Alert.Answer(positiveAnswerResId).onSelect {
                    onSelect?.invoke(DialogInterface.BUTTON_POSITIVE)
                })
                add(Alert.Answer(negativeAnswerResId).onSelect {
                    onSelect?.invoke(DialogInterface.BUTTON_NEGATIVE)
                })
                neutralAnswerResId?.let {
                    add(Alert.Answer(neutralAnswerResId).onSelect {
                        onSelect?.invoke(DialogInterface.BUTTON_NEUTRAL)
                    })
                }
            }
        )
        .build()
}