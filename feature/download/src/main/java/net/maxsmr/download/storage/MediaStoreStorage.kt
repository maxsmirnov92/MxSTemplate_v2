package net.maxsmr.download.storage

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import net.maxsmr.commonutils.IStreamNotifier
import net.maxsmr.commonutils.copyStreamOrThrow
import net.maxsmr.commonutils.media.length
import net.maxsmr.commonutils.media.lengthOrThrow
import net.maxsmr.commonutils.media.openOutputStreamOrThrow
import net.maxsmr.download.DownloadService
import java.io.InputStream
import java.io.OutputStream

/**
 * Используется для версий >= Q для сохранения файлов в shared области памяти
 */
@RequiresApi(Build.VERSION_CODES.Q)
class MediaStoreStorage(
    context: Context,
) : DownloadServiceStorage(context, Type.SHARED) {

    private val resolver get() = context.contentResolver

    private val idColumn: String
        get() = MediaStore.Downloads._ID
    private val nameColumn: String
        get() = MediaStore.Downloads.DISPLAY_NAME
    private val pathColumn: String
        get() = MediaStore.Downloads.RELATIVE_PATH

    private val uriWritable
        get() = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    private val uriReadOnly
        get() = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)

    override fun store(
        params: DownloadService.Params,
        targetUri: Uri?,
        writeStreamFunc: (Uri, OutputStream, Long) ->  Unit
    ): Uri {

        fun Uri.tryUpdate(values: ContentValues): Uri? = try {
            resolver.update(this, values, null, null)
            this
        } catch (e: Exception) {
            null
        }

        var uri: Uri? = null
        try {
            val dirPath = type.dirPath(context, params.subDirPath)
            val uniqueName = uniqueNameFor(dirPath, params.targetResourceName)
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, uniqueName)
                val mimeType = params.resourceMimeType
                if (mimeType.isNotEmpty()) {
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                }
                put(MediaStore.Downloads.DOWNLOAD_URI, params.requestParams.url)
                put(pathColumn, dirPath)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            uri = targetUri?.tryUpdate(values) ?: resolver.insert(uriWritable, values)
            if (uri == null) {
                resolver.delete(
                    uriWritable,
                    "$pathColumn = ? AND $nameColumn = ?",
                    arrayOf(dirPath, uniqueName)
                )
                uri = resolver.insert(uriWritable, values)
            }
            uri ?: throw KotlinNullPointerException("Cannot insert uri to MediaStore")
            writeStreamFunc(uri, uri.openOutputStreamOrThrow(contentResolver), uri.lengthOrThrow(resolver))
            val pendingValues = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            resolver.update(uri, pendingValues, null, null)
            return uri
        } catch (e: Exception) {
            throw e.wrapIfNeed(uri)
        }
    }

    override fun namesAt(fullDirPath: String, startsWith: String?): Set<UriAndName> {
        val projection = arrayOf(idColumn, nameColumn)
        val selection = startsWith?.takeIf { it.isNotBlank() }
            ?.let { "$pathColumn = ? AND $nameColumn LIKE '${it}%'" }
            ?: "$pathColumn = ?"
        val selectionArgs = arrayOf(fullDirPath)

        val query = resolver.query(
            uriWritable,
            projection,
            selection,
            selectionArgs,
            null
        )
        val names = mutableSetOf<UriAndName>()
        query?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(idColumn)
            val nameIndex = cursor.getColumnIndexOrThrow(nameColumn)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val name = cursor.getString(nameIndex)
                val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                val length = uri.length(contentResolver)
                if (length > 0) {
                    names.add(UriAndName(uri, name))
                } else {
                    resolver.delete(
                        uriWritable,
                        "$pathColumn = ? AND $nameColumn = ?",
                        arrayOf(fullDirPath, name)
                    )
                }
            }
        }
        return names
    }
}