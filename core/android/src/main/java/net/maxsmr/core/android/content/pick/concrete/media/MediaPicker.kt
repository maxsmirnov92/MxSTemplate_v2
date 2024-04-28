package net.maxsmr.core.android.content.pick.concrete.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import net.maxsmr.commonutils.media.takePersistableReadPermission
import net.maxsmr.core.android.content.mediaStoreExternalContentUri
import net.maxsmr.core.android.content.pick.concrete.ConcretePicker

/**
 * Пикер для взятия уже существующего медиа контента через MediaStore API.
 */
internal class MediaPicker : ConcretePicker<MediaPickerParams> {

    override fun intent(params: MediaPickerParams, context: Context): Intent {
        return Intent(Intent.ACTION_PICK, params.contentType.mediaStoreExternalContentUri).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    override fun requiredPermissions(params: MediaPickerParams, context: Context): Array<String> = params.requiredPermissions

    override fun onPickResult(
        params: MediaPickerParams,
        uri: Uri?,
        needPersistableAccess: Boolean,
        context: Context,
    ): Uri? =
        uri?.apply {
            if (needPersistableAccess) takePersistableReadPermission(context.contentResolver)
        }
}