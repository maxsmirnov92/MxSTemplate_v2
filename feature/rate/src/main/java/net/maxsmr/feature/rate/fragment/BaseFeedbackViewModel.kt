package net.maxsmr.feature.rate.fragment

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import net.maxsmr.commonutils.flow.field.Field
import net.maxsmr.commonutils.flow.field.anyRequiredFieldEmptyFlow
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.field.createTextField
import net.maxsmr.core.ui.openEmailIntentWithToastError
import net.maxsmr.feature.rate.R

abstract class BaseFeedbackViewModel(state: SavedStateHandle, context: Context,) : BaseViewModel(state, context) {

    protected abstract val emailAddress: String

    val subjectField: Field<String> = createTextField(
        initialValue = EMPTY_STRING,
        key = KEY_FIELD_SUBJECT
    ) {
        setRequired(true, R.string.rate_feedback_field_subject_empty_error)
        hint(R.string.rate_feedback_field_subject_hint, withAsterisk = false)
    }

    val textField: Field<String> = createTextField(
        initialValue = EMPTY_STRING,
        key = KEY_FIELD_TEXT
    ) {
        setRequired(true, R.string.rate_feedback_field_text_empty_error)
        hint(R.string.rate_feedback_field_message_hint, withAsterisk = false)
    }

    val isSendEnabled = listOf(subjectField, textField)
        .anyRequiredFieldEmptyFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun openEmailIntent(context: Context) {
        if (!isSendEnabled.value) return
        val subject = subjectField.value
        val text = textField.value
        context.openEmailIntentWithToastError(emailAddress, sendIntentFunc = {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        })
    }

    companion object {

        private const val KEY_FIELD_SUBJECT = "subject"
        private const val KEY_FIELD_TEXT = "text"
    }
}