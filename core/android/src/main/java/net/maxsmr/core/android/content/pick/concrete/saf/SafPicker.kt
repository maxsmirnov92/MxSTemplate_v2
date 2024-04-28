package net.maxsmr.core.android.content.pick.concrete.saf

import android.content.Context
import android.content.Intent
import android.net.Uri
import net.maxsmr.commonutils.getOpenDocumentIntent
import net.maxsmr.commonutils.media.takePersistableReadPermission
import net.maxsmr.core.android.content.pick.concrete.ConcretePicker

/**
 * Пикер для взятия уже существующего контента через SAF API (Storage Access Framework).
 */
internal class SafPicker : ConcretePicker<SafPickerParams> {

    override fun intent(params: SafPickerParams, context: Context): Intent =
        getOpenDocumentIntent(params.intentType, params.mimeTypes).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    override fun requiredPermissions(params: SafPickerParams, context: Context): Array<String> = params.requiredPermissions

    override fun onPickResult(
        params: SafPickerParams,
        uri: Uri?,
        needPersistableAccess: Boolean,
        context: Context,
    ): Uri? =
        uri?.apply {
            if (needPersistableAccess) {
                takePersistableReadPermission(context.contentResolver)
            }
        }
}