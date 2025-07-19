package net.maxsmr.core.ui.view

import android.widget.CompoundButton
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.textfield.TextInputLayout
import net.maxsmr.commonutils.flow.field.Field
import net.maxsmr.commonutils.flow.observe
import net.maxsmr.commonutils.gui.bindTo
import net.maxsmr.commonutils.gui.setCheckedDistinct

fun Field<Boolean>.bindValue(lifecycleOwner: LifecycleOwner, checkBox: CompoundButton) {
    checkBox.bindTo(this)
    valueFlow.observe(lifecycleOwner) {
        checkBox.setCheckedDistinct(it)
    }
}

fun Field<Boolean>.bindValueWithState(
    lifecycleOwner: LifecycleOwner,
    compoundButton: CompoundButton,
    hideIfDisabled: Boolean = false,
) {
    compoundButton.setOnCheckedChangeListener { _, isChecked ->
        value = isChecked
    }
    valueFlow.observe(lifecycleOwner) {
        compoundButton.setCheckedDistinct(value)
        compoundButton.isEnabled = enabled
        if (hideIfDisabled) {
            compoundButton.isVisible = enabled
        }
    }
}

fun <D> Field<D>.bindHintError(
    lifecycleOwner: LifecycleOwner,
    textInputLayout: TextInputLayout,
) {
    hintFlow.observe(lifecycleOwner) {
        textInputLayout.hint = it?.get(textInputLayout.context)
    }
    errorFlow.observe(lifecycleOwner) {
        textInputLayout.error = it?.get(textInputLayout.context)
    }
}

fun <D> Field<D>.bindHintError(
    lifecycleOwner: LifecycleOwner,
    editText: EditText,
) {
    hintFlow.observe(lifecycleOwner) {
        editText.hint = it?.get(editText.context)
    }
    errorFlow.observe(lifecycleOwner) {
        editText.error = it?.get(editText.context)
    }
}