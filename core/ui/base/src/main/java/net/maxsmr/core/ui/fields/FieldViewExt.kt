package net.maxsmr.core.ui.fields

import android.widget.CompoundButton
import android.widget.EditText
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.textfield.TextInputLayout
import net.maxsmr.commonutils.gui.bindTo
import net.maxsmr.commonutils.gui.setCheckedDistinct
import net.maxsmr.commonutils.live.field.Field
import java.io.Serializable

typealias BooleanFieldWithState = FieldWithState<Boolean>
typealias StringFieldWithState = FieldWithState<String>
typealias IntFieldWithState = FieldWithState<Int>
typealias LongFieldWithState = FieldWithState<Long>

fun Field<Boolean>.bindValue(lifecycleOwner: LifecycleOwner, checkBox: CompoundButton) {
    checkBox.bindTo(this)
    this.valueLive.observe(lifecycleOwner) {
        checkBox.setCheckedDistinct(it)
    }
}

fun Field<BooleanFieldWithState>.bindValueWithState(
    lifecycleOwner: LifecycleOwner,
    compoundButton: CompoundButton,
    hideIfDisabled: Boolean = false
) {
    compoundButton.setOnCheckedChangeListener { _, isChecked ->
        this.setFieldValueIfEnabled(isChecked)
    }
    this.valueLive.observe(lifecycleOwner) {
        compoundButton.setCheckedDistinct(it.value)
        compoundButton.isEnabled = it.isEnabled
        if (hideIfDisabled) {
            compoundButton.isVisible = it.isEnabled
        }
    }
}

fun <D> Field<D>.bindHintError(
    lifecycleOwner: LifecycleOwner,
    textInputLayout: TextInputLayout
    ) {
    hintLive.observe(lifecycleOwner) {
        textInputLayout.hint = it?.get(textInputLayout.context)
    }
    errorLive.observe(lifecycleOwner) {
        textInputLayout.error = it?.get(textInputLayout.context)
    }
}

fun <D> Field<D>.bindHintError(
    lifecycleOwner: LifecycleOwner,
    editText: EditText
) {
    hintLive.observe(lifecycleOwner) {
        editText.hint = it?.get(editText.context)
    }
    errorLive.observe(lifecycleOwner) {
        editText.error = it?.get(editText.context)
    }
}


fun <D : Serializable> Field<FieldWithState<D>>.setFieldValueIfEnabled(value: D) {
    val flags = this.value /*?: FieldState(value = value)*/
    if (flags.isEnabled) {
        this.value = flags.copy(value = value)
    }
}

fun <D : Serializable> Field<FieldWithState<D>>.toggleRequiredFieldState(required: Boolean, @StringRes errorResId: Int) {
    val currentValue: FieldWithState<D> = value /*?: defaultValue*/
    value = if (required) {
        this.setRequired(errorResId)
        currentValue.copy(isEnabled = true)
    } else {
        setNonRequired()
        currentValue.copy(isEnabled = false)
    }
}

data class FieldWithState<D : Serializable>(
    val value: D,
    val isEnabled: Boolean = false,
) : Serializable