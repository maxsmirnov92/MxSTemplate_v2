package net.maxsmr.core.ui.field

import androidx.annotation.StringRes
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.flow.field.Field
import java.io.Serializable

/**
 * Меняет обязательность поля вместе с доступностью
 */
fun <D : Serializable> Field<D>.toggleRequiredWithEnabled(
    required: Boolean,
    @StringRes emptyMessageResId: Int
) {
    if (required) {
        setRequired(true, TextMessage(emptyMessageResId))
        enabled = true
    } else {
        setNonRequired()
        enabled = false
    }
}
