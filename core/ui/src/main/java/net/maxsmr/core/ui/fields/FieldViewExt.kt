package net.maxsmr.core.ui.fields

import android.widget.CompoundButton
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.textfield.TextInputLayout
import net.maxsmr.commonutils.gui.bindTo
import net.maxsmr.commonutils.gui.setCheckedDistinct
import net.maxsmr.commonutils.live.field.Field
import java.io.Serializable

typealias BooleanFieldState = FieldState<Boolean>
typealias StringFieldState = FieldState<String>
typealias IntFieldState = FieldState<Int>
typealias LongFieldState = FieldState<Long>

fun Field<Boolean>.bindValue(lifecycleOwner: LifecycleOwner, checkBox: CompoundButton) {
    checkBox.bindTo(this)
    this.valueLive.observe(lifecycleOwner) {
        checkBox.setCheckedDistinct(it)
    }
}

fun Field<BooleanFieldState>.bindState(lifecycleOwner: LifecycleOwner, checkBox: CompoundButton) {
    checkBox.setOnCheckedChangeListener { _, isChecked ->
        this.toggleFieldState(isChecked)
    }
    this.valueLive.observe(lifecycleOwner) {
        checkBox.setCheckedDistinct(it.value)
        checkBox.isEnabled = it.isEnabled
    }
}

fun <D> Field<D>.bindHintError(lifecycleOwner: LifecycleOwner, textInputLayout: TextInputLayout) {
    hintLive.observe(lifecycleOwner) {
        textInputLayout.hint = it?.get(textInputLayout.context)
    }
    errorLive.observe(lifecycleOwner) {
        textInputLayout.error = it?.get(textInputLayout.context)
    }
}


fun <D : Serializable> Field<FieldState<D>>.toggleFieldState(value: D) {
    val flags = this.value ?: FieldState(value = value)
    if (flags.isEnabled) {
        this.value = flags.copy(value = value)
    }
}

fun <D : Serializable> Field<FieldState<D>>.toggleRequiredFieldState(
    required: Boolean,
    @StringRes errorResId: Int,
    defaultValue: FieldState<D>,
) {
    val currentValue: FieldState<D> = value ?: defaultValue
    value = if (required) {
        this.setRequired(errorResId)
        currentValue.copy(isEnabled = true)
    } else {
        setNonRequired()
        currentValue.copy(isEnabled = false)
    }
}

data class FieldState<D : Serializable>(
    val value: D,
    val isEnabled: Boolean = false,
) : Serializable