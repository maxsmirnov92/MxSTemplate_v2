package net.maxsmr.core.ui.field

import androidx.annotation.StringRes
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.flow.field.Field
import java.io.Serializable

typealias BooleanFieldWithState = FieldWithState<Boolean>
typealias StringFieldWithState = FieldWithState<String>
typealias IntFieldWithState = FieldWithState<Int>
typealias LongFieldWithState = FieldWithState<Long>

fun <D : Serializable> Field<FieldWithState<D>>.setFieldValueIfEnabled(value: D) {
    val flags = this.value /*?: FieldState(value = value)*/
    if (flags.isEnabled) {
        this.value = flags.copy(value = value)
    }
}

fun <D : Serializable> Field<FieldWithState<D>>.toggleRequiredFieldState(
    required: Boolean,
    @StringRes errorResId: Int
) {
    val currentValue: FieldWithState<D> = value /*?: defaultValue*/
    value = if (required) {
        this.setRequired(required, TextMessage(errorResId))
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