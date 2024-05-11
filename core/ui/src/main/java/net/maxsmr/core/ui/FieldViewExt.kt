package net.maxsmr.core.ui

import android.widget.CompoundButton
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.textfield.TextInputLayout
import net.maxsmr.commonutils.gui.bindTo
import net.maxsmr.commonutils.gui.setCheckedDistinct
import net.maxsmr.commonutils.live.field.Field
import java.io.Serializable

fun Field<Boolean>.bindValue(lifecycleOwner: LifecycleOwner, checkBox: CompoundButton) {
    checkBox.bindTo(this)
    this.valueLive.observe(lifecycleOwner) {
        checkBox.setCheckedDistinct(it)
    }
}

fun Field<BooleanFieldFlags>.bindFlags(lifecycleOwner: LifecycleOwner, checkBox: CompoundButton) {
    checkBox.setOnCheckedChangeListener { _, isChecked ->
        this.toggleBooleanFieldFlags(isChecked)
    }
    this.valueLive.observe(lifecycleOwner) {
        checkBox.setCheckedDistinct(it.state)
        checkBox.isEnabled = it.isEnabled
    }
}

fun <D> Field<D>.bindHintError(lifecycleOwner: LifecycleOwner, textInputLayout: TextInputLayout){
    hintLive.observe(lifecycleOwner) {
        textInputLayout.hint = it?.get(textInputLayout.context)
    }
    errorLive.observe(lifecycleOwner) {
        textInputLayout.error = it?.get(textInputLayout.context)
    }
}

private fun Field<BooleanFieldFlags>.toggleBooleanFieldFlags(toggle: Boolean) {
    val flags = this.value ?: BooleanFieldFlags()
    if (flags.isEnabled) {
        this.value = flags.copy(state = toggle)
    }
}

data class BooleanFieldFlags(
    val state: Boolean = false,
    val isEnabled: Boolean = false,
) : Serializable