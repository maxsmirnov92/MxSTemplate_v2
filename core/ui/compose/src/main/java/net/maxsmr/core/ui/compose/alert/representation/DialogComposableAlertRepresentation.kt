package net.maxsmr.core.ui.compose.alert.representation

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import net.maxsmr.commonutils.ISpanInfo
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.ui.alert.representation.BaseDialogAlertRepresentationBuilder
import net.maxsmr.core.ui.compose.alert.representation.DialogComposableAlertRepresentationBuilder.MultiChoiceAnswersData.Companion.isNotEmpty
import net.maxsmr.designsystem.compose.component.CheckBoxWithText
import net.maxsmr.designsystem.compose.component.RadioButtonWithText

class DialogComposableAlertRepresentationBuilder(
    private val context: Context,
    alert: Alert,
) : BaseDialogAlertRepresentationBuilder<ComposableAlertRepresentation>(alert) {

    private var customConfigBlock: @Composable (() -> Unit)? = null

    private var multiChoiceAnswers: MultiChoiceAnswersData? = null
    private var onMultiChoiceClick: ((List<Alert.Answer>) -> Unit)? = null

    fun setTitleSpans(vararg spans: ISpanInfo?) = apply {
        this.titleSpans = spans.filterNotNull()
    }

    fun setMessageSpans(vararg spans: ISpanInfo?) = apply {
        this.messageSpans = spans.filterNotNull()
    }

    fun setCancelable(cancelable: Boolean) = apply {
        this.cancelable = cancelable
    }

    fun setOnCancelListener(onCancel: () -> Unit) = apply {
        this.onCancel = onCancel
    }

    fun setPositiveButton(answer: Alert.Answer, onClick: (() -> Unit)? = null) = apply {
        positiveAnswer = answer
        onPositiveClick = onClick
    }

    fun setNegativeButton(answer: Alert.Answer, onClick: (() -> Unit)? = null) = apply {
        negativeAnswer = answer
        onNegativeClick = onClick
    }

    fun setMultiChoiceAnswers(
        answers: MultiChoiceAnswersData,
        onClick: ((answers: List<Alert.Answer>) -> Unit)? = null,
    ) = apply {
        multiChoiceAnswers = answers
        onMultiChoiceClick = onClick
    }

    fun setOnDismiss(onDismiss: () -> Unit) = apply {
        this.onDismiss = onDismiss
    }

    override fun build(): ComposableAlertRepresentation {
        // TODO neutral button
        val hasAnyAnswer = positiveAnswer != null
                || negativeAnswer != null
                || multiChoiceAnswers.isNotEmpty()
        check(hasAnyAnswer || cancelable) {
            "Cannot create non cancelable dialog without answers. How to dismiss it?"
        }
        val title = alert.title?.format(context, titleSpans)?.toString().orEmpty()
        val message = alert.message?.format(context, messageSpans)?.toString().orEmpty()
        return ComposableAlertRepresentation {
            customConfigBlock?.invoke() ?: AlertDialog(
                onDismissRequest = {
                    alert.close()
                    onCancel?.invoke()
                },
                title = {
                    Text(title)
                },
                text = {
                    Column {
                        val answersState = remember { mutableStateOf(multiChoiceAnswers?.answers.orEmpty()) }
                        val answers = answersState.value
                        if (answers.isEmpty()) {
                            Text(message)
                        } else {
                            val isRadioButton = multiChoiceAnswers?.isRadioButton ?: false
                            LazyColumn(
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(answers, {
                                    it.tag ?: it.title.get(context)
                                }) {
                                    val isChecked = it.isChecked ?: false
                                    val text = it.title.get(context).toString()
                                    if (isRadioButton) {
                                        RadioButtonWithText(isChecked, text, {
                                            answersState.value = answers.map { a ->
                                                if (a != it) {
                                                    a.copy(isChecked = false)
                                                } else {
                                                    a.copy(isChecked = true)
                                                }
                                            }
                                            if (it.closeAfterSelect) {
                                                alert.doClose()
                                            }
                                        })
                                    } else {
                                        CheckBoxWithText(isChecked, text, { checked ->
                                            answersState.value = answers.map { a ->
                                                if (a != it) {
                                                    a
                                                } else {
                                                    a.copy(isChecked = checked)
                                                }
                                            }
                                        })
                                    }
                                }
                            }
                        }

                    }
                },
                confirmButton = {
                    positiveAnswer?.AnswerTextButton {
                        val checkedAnswers = multiChoiceAnswers?.answers.orEmpty().filter { it.isChecked == true }
                        if (checkedAnswers.isNotEmpty()) {
                            checkedAnswers.forEach {
                                it.select?.invoke()
                            }
                            onMultiChoiceClick?.invoke(checkedAnswers)
                        }
                        onPositiveClick?.invoke()
                    }
                },
                dismissButton = {
                    negativeAnswer?.AnswerTextButton(onNegativeClick)
                },
                properties = DialogProperties(
                    dismissOnBackPress = cancelable,
                    dismissOnClickOutside = cancelable
                ),

            )
        }
    }

    private fun Alert.doClose() {
        close()
        onDismiss?.invoke()
    }

    @Composable
    private fun Alert.Answer.AnswerTextButton(onClick: (() -> Unit)?) {
        TextButton({
            onClick?.invoke()
            if (closeAfterSelect) {
                alert.doClose()
            }
        }) {
            Text(title.get(context).toString())
        }
    }

    data class MultiChoiceAnswersData(
        val answers: List<Alert.Answer> = emptyList(),
        val isRadioButton: Boolean,
    ) {

        val isEmpty = answers.isEmpty()

        companion object {

            fun MultiChoiceAnswersData?.isNotEmpty() = this != null && !this.isEmpty
        }
    }
}