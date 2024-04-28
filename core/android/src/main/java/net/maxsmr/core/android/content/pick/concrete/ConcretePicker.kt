package net.maxsmr.core.android.content.pick.concrete

import android.content.Context
import android.content.Intent
import android.net.Uri
import net.maxsmr.core.android.content.pick.IntentWithPermissions

/**
 * Пикер контента конкретного типа
 */
internal interface ConcretePicker<P : ConcretePickerParams> {

    fun intentWithPermissions(params: P, context: Context): IntentWithPermissions =
        IntentWithPermissions(intent(params, context), requiredPermissions(params, context))

    fun intent(params: P, context: Context): Intent

    fun requiredPermissions(params: P, context: Context): Array<String>

    fun onPickResult(params: P, uri: Uri?, needPersistableAccess: Boolean, context: Context): Uri?

    fun onPickCancelled(context: Context) {}
}