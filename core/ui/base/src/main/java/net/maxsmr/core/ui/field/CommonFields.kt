package net.maxsmr.core.ui.field

import androidx.annotation.StringRes
import net.maxsmr.commonutils.REG_EX_FILE_NAME
import net.maxsmr.commonutils.flow.field.Field
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.network.isUrlValid
import net.maxsmr.core.ui.R

fun BaseViewModel.urlField(
    @StringRes hintResId: Int,
    isRequired: Boolean = false,
    initialValue: String = EMPTY_STRING,
    withAsterisk: Boolean = true,
    isValidByBlank: Boolean = false,
    isNonResource: Boolean = true,
    schemeIfEmpty: String = EMPTY_STRING,
    key: String = KEY_FIELD_URL,
): Field<String> = createTextField(
    initialValue = initialValue,
    key = key,
    valueGetter = { it.trim() }
) {
    validators(Field.Validator(R.string.field_url_error) {
        it.isUrlValid(
            orBlank = isValidByBlank,
            schemeIfEmpty = schemeIfEmpty,
            isNonResource = isNonResource
        )
    })
    hint(hintResId, withAsterisk = withAsterisk)
    if (isRequired) {
        setRequired(true, R.string.field_url_empty_error)
    }
}

fun BaseViewModel.fileNameField(
    isRequired: Boolean = false,
    initialValue: String = EMPTY_STRING,
): Field<String> = createTextField(
    initialValue = initialValue,
    key = KEY_FIELD_FILE_NAME
) {
    validators(Field.Validator(R.string.field_file_name_error) { Regex(REG_EX_FILE_NAME).matches(it) })
    hint(R.string.field_file_name_hint)
    if (isRequired) {
        setRequired(true, R.string.field_file_name_empty_error)
    }
}

fun BaseViewModel.subDirNameField(
    isRequired: Boolean = false,
    initialValue: String = EMPTY_STRING,
): Field<String> = createTextField(
    initialValue = initialValue,
    key = KEY_FIELD_SUB_DIR_NAME,
) {
    validators(Field.Validator(R.string.field_sub_dir_name_error) { Regex(REG_EX_FILE_NAME).matches(it) })
    hint(R.string.field_sub_dir_name_hint)
    if (isRequired) {
        setRequired(true, R.string.field_sub_dir_name_empty_error)
    }
}

private const val KEY_FIELD_URL = "url"
private const val KEY_FIELD_FILE_NAME = "file_name"
private const val KEY_FIELD_SUB_DIR_NAME = "sub_dir_name"