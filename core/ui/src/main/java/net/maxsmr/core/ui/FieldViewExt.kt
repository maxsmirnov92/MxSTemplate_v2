package net.maxsmr.core.ui

import android.widget.CompoundButton
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.textfield.TextInputLayout
import net.maxsmr.commonutils.gui.bindTo
import net.maxsmr.commonutils.gui.setCheckedDistinct
import net.maxsmr.commonutils.live.field.Field

fun Field<Boolean>.bindValue(lifecycleOwner: LifecycleOwner, checkBox: CompoundButton) {
    checkBox.bindTo(this)
    this.valueLive.observe(lifecycleOwner) {
        checkBox.setCheckedDistinct(it)
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