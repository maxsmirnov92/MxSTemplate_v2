package net.maxsmr.core.ui.fields

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import net.maxsmr.commonutils.REG_EX_FILE_NAME
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.network.isUrlValid
import net.maxsmr.core.ui.R

fun SavedStateHandle.urlField(
    @StringRes hintResId: Int,
    isRequired: Boolean = false,
    initialValue: String = EMPTY_STRING,
    withAsterisk: Boolean = true,
    isValidByBlank: Boolean = false
    ): Field<String> = object : Field.Builder<String>(initialValue) {
    override fun valueGetter(fieldValue: MutableLiveData<String>): () -> String = {
        fieldValue.value.orEmpty().trim()
    }
}.emptyIf { it.isEmpty() }
    .validators(Field.Validator(R.string.field_url_error) {
        it.isUrlValid(orBlank = isValidByBlank)
    })
    .hint(hintResId, withAsterisk = withAsterisk)
    .persist(this, KEY_FIELD_URL)
    .apply {
        if (isRequired) {
            setRequired(R.string.field_url_empty_error)
        }
    }
    .build()

fun SavedStateHandle.fileNameField(
    isRequired: Boolean = false,
    initialValue: String = EMPTY_STRING,
): Field<String> = Field.Builder(initialValue)
    .emptyIf { it.isEmpty() }
    .validators(Field.Validator(R.string.field_file_name_error) { Regex(REG_EX_FILE_NAME).matches(it) })
    .hint(R.string.field_file_name_hint)
    .persist(this, KEY_FIELD_FILE_NAME)
    .apply {
        if (isRequired) {
            setRequired(R.string.field_file_name_empty_error)
        }
    }
    .build()

fun SavedStateHandle.subDirNameField(
    isRequired: Boolean = false,
    initialValue: String = EMPTY_STRING,
): Field<String> = Field.Builder(initialValue)
    .emptyIf { it.isEmpty() }
    .validators(Field.Validator(R.string.field_sub_dir_name_error) { Regex(REG_EX_FILE_NAME).matches(it) })
    .hint(R.string.field_sub_dir_name_hint)
    .persist(this, KEY_FIELD_SUB_DIR_NAME)
    .apply {
        if (isRequired) {
            setRequired(R.string.field_sub_dir_name_empty_error)
        }
    }
    .build()

private const val KEY_FIELD_URL = "url"
private const val KEY_FIELD_FILE_NAME = "file_name"
private const val KEY_FIELD_SUB_DIR_NAME = "sub_dir_name"