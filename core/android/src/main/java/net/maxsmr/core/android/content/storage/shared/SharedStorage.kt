package net.maxsmr.core.android.content.storage.shared

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.github.kittinunf.result.Result
import net.maxsmr.core.android.content.ContentType
import net.maxsmr.core.android.content.contentUri
import net.maxsmr.core.android.content.mediaStoreDisplayName
import net.maxsmr.core.android.content.mediaStoreExternalContentUri
import net.maxsmr.core.android.content.mediaStoreId
import net.maxsmr.core.android.content.rootDir
import java.io.File
import java.io.IOException

/**
 * Внещнее Shareable хранилище файлов. Характеристики:
 *
 * 1. Объем хранилища - большой.
 * 1. Разрешения на доступ:
 *      1. Android 11 (API level 30) - не нужно для доступа к файлам, созданным этим приложением.
 *      Для доступа к файлам, созданным другими приложениями требуется READ_EXTERNAL_STORAGE.
 *      1. Android 10 (API level 29), Scoped Storage **enabled** - не нужно для доступа к файлам, созданным этим приложением.
 *      Для доступа к файлам, созданным другими приложениями требуется READ_EXTERNAL_STORAGE или WRITE_EXTERNAL_STORAGE.
 *      1. Android 9- (API level pre 29) OR Scoped Storage **disabled** (requestLegacyExternalStorage флаг в манифесте) -
 *      требуется READ_EXTERNAL_STORAGE или WRITE_EXTERNAL_STORAGE для доступа к **любым** файлам.
 * 1. Приватность данных - данные доступны другим приложениям с запросом READ_EXTERNAL_STORAGE разрешения.
 * 1. Доступность хранилища - всегда доступно.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class SharedStorage private constructor(
        contentType: ContentType,
        context: Context,
) : AbsSharedStorage(contentType, context) {

    override fun get(name: String, path: String?, context: Context): Result<Uri, Exception> = Result.of {
        val projection = arrayOf(contentType.mediaStoreId)

        val selection = "${contentType.mediaStoreDisplayName} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(name, path())

        val query = resolver.query(
                contentType.contentUri(false),
                projection,
                selection,
                selectionArgs,
                null
        )
        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(contentType.mediaStoreId)

            if (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                return@of ContentUris.withAppendedId(contentType.mediaStoreExternalContentUri, id)
            }
        }
        throw IOException("Cannot get content with `$name`")
    }

    override fun create(name: String, path: String?, context: Context): Result<Uri, Exception> = Result.of {
        val pathHardcoded = path()
        val values = ContentValues().apply {
            put(contentType.mediaStoreDisplayName, name)
            put(MediaStore.MediaColumns.RELATIVE_PATH, pathHardcoded)
        }
        val contentUri = contentType.contentUri(true)
        //Удаляем существующий, если есть, т.к. во-первых это соответствует контракту метода (см. доку родителя),
        //во-вторых если удалить файл и не оповестить MediaStore об этом, повторный insert не сработает
        resolver.delete(
            contentUri,
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${contentType.mediaStoreDisplayName} = ?",
            arrayOf(pathHardcoded, name)
        )
        resolver.insert(contentUri, values)
                ?: throw IOException("Cannot create content with `$name`")
    }

    override fun path(): String {
        return "${contentType.rootDir}${File.separator}$appDir${File.separator}"
    }

    override fun requiredPermissions(read: Boolean, write: Boolean): Array<String> {
        return listOfNotNull(
                Manifest.permission.READ_EXTERNAL_STORAGE.takeIf { read },
                Manifest.permission.WRITE_EXTERNAL_STORAGE.takeIf { write },
        ).toTypedArray()
    }

    companion object {

        @JvmStatic
        fun create(type: ContentType, context: Context): AbsSharedStorage {
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                SharedStorageLegacy(type, context)
            } else {
                SharedStorage(type, context)
            }
        }
    }
}