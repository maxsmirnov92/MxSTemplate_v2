package net.maxsmr.core.android.content.storage.shared

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.onSuccess
import net.maxsmr.core.android.content.ContentType
import net.maxsmr.core.android.content.rootDir
import net.maxsmr.core.android.content.storage.UriStorageAdapter
import net.maxsmr.core.android.content.storage.app_private.ExternalFileStorage


/**
 * Аналог [SharedStorage] с единственным отличием - использует File API вместо MediaStore API.
 * Используется на андроид < 10.
 *
 * Причин для существования 2:
 * 1. MediaStore API позволяет делать относительные пути только с Android 10 (таблица MediaStore.MediaColumns.RELATIVE_PATH)
 * 1. MediaStore API позволяет работать с немедиафайлами только с Android 10 (таблица MediaStore.Downloads)
 */
@Suppress("DEPRECATION")
internal class SharedStorageLegacy(
    contentType: ContentType,
    context: Context,
) : AbsSharedStorage(contentType, context) {

    override val path: String = appDir

    private val srcStorage = UriStorageAdapter(
        ExternalFileStorage(
            Environment.getExternalStoragePublicDirectory(contentType.rootDir),
            context,
        ), context = context
    )

    override fun get(name: String, path: String?): Result<Uri, Exception> {
        return srcStorage.get(name, this.path)
    }

    override fun create(name: String, path: String?): Result<Uri, Exception> {
        return srcStorage.create(name, this.path).onSuccess {
            it.notifyChange(context)
        }
    }

    override fun delete(resource: Uri): Result<Boolean, Exception> {
        return srcStorage.delete(resource).onSuccess {
            resource.notifyChange(context)
        }
    }

    override fun requiredPermissions(read: Boolean, write: Boolean): Array<String> {
        return listOfNotNull(
            Manifest.permission.READ_EXTERNAL_STORAGE.takeIf { read },
            Manifest.permission.WRITE_EXTERNAL_STORAGE.takeIf { write },
        ).toTypedArray()
    }

    private fun Uri.notifyChange(context: Context) {
        val scanFileIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, this)
        context.sendBroadcast(scanFileIntent)
    }
}