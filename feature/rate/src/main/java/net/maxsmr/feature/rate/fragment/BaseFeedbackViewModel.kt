package net.maxsmr.feature.rate.fragment

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.live.zip
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.core.ui.openEmailIntentWithToastError
import net.maxsmr.feature.rate.R

abstract class BaseFeedbackViewModel(state: SavedStateHandle) : BaseHandleableViewModel(state) {

    protected abstract val emailAddress: String

    val subjectField: Field<String> = Field.Builder(EMPTY_STRING)
        .emptyIf { it.isEmpty() }
        .setRequired(R.string.rate_feedback_field_subject_empty_error)
        .hint(R.string.rate_feedback_field_subject_hint, withAsterisk = false)
        .persist(state, KEY_FIELD_SUBJECT)
        .build()

    val textField: Field<String> = Field.Builder(EMPTY_STRING)
        .emptyIf { it.isEmpty() }
        .setRequired(R.string.rate_feedback_field_text_empty_error)
        .hint(R.string.rate_feedback_field_text_hint, withAsterisk = false)
        .persist(state, KEY_FIELD_TEXT)
        .build()

    val isSendEnabled = zip(subjectField.isEmptyLive, textField.isEmptyLive) { e1, e2 ->
        e1 == false && e2 == false
    }

    fun openEmailIntent(context: Context) {
        if (isSendEnabled.value != true) return
        val subject = subjectField.value
        val text = textField.value
        context.openEmailIntentWithToastError(emailAddress, sendIntentFunc = {
            it.putExtra(Intent.EXTRA_SUBJECT, subject)
            it.putExtra(Intent.EXTRA_TEXT, text)
        })
    }

    companion object {

        private const val KEY_FIELD_SUBJECT = "subject"
        private const val KEY_FIELD_TEXT = "text"
    }
}