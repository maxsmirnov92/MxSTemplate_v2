package net.maxsmr.download.storage

import android.content.Context
import android.net.Uri
import net.maxsmr.commonutils.FileLockInfo
import net.maxsmr.commonutils.IStreamNotifier
import net.maxsmr.commonutils.copyStreamOrThrow
import net.maxsmr.commonutils.createFileOrThrow
import net.maxsmr.commonutils.lock
import net.maxsmr.commonutils.media.delete
import net.maxsmr.commonutils.media.lengthOrThrow
import net.maxsmr.commonutils.media.openOutputStreamOrThrow
import net.maxsmr.commonutils.media.scanFiles
import net.maxsmr.commonutils.media.toContentUri
import net.maxsmr.commonutils.releaseSafe
import net.maxsmr.commonutils.renameFileOrThrow
import net.maxsmr.download.DownloadService
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Используется для сохранения файлов в app-private и external области памяти, а также
 * в shared область памяти для версий < Q
 */
class FileStorage(
    context: Context,
    type: Type,
) : DownloadServiceStorage(context, type) {

    override fun store(
        params: DownloadService.Params,
        targetUri: Uri?,
        writeStreamFunc: (Uri, OutputStream, Long) -> Unit,
    ): Uri {
        return if (targetUri != null) {
            try {
                targetUri.delete(contentResolver)
                writeStreamFunc(targetUri, targetUri.openOutputStreamOrThrow(contentResolver), targetUri.lengthOrThrow(contentResolver))
                targetUri
            } catch (e: Exception) {
                throw e.wrapIfNeed(targetUri)
            }
        } else {
            var tempLock: FileLockInfo? = null
            var uri: Uri? = null
            try {
                val dirPath = type.dirPath(context, params.subDirPath)
                val uniqueName = uniqueNameFor(dirPath, params.targetResourceName)

                val tempFileName = "$uniqueName.$EXT_TEMP_FILE"
                val tempFile = createFileOrThrow(tempFileName, dirPath, true)
                tempLock = tempFile.lock()

                uri = tempFile.toContentUri(context)
                writeStreamFunc(uri, uri.openOutputStreamOrThrow(contentResolver), uri.lengthOrThrow(contentResolver))
                tempLock?.releaseSafe()
                // стрим затянут до конца -> переименовываем файл в исходный
                // (или с номером, если уже существует)
                val newFile = renameFileOrThrow(
                    tempFile,
                    tempFile.parent,
                    uniqueName,
                    false,
                    false
                )
                scanFiles(context, listOf(newFile))
                newFile.toContentUri(context)
            } catch (e: Exception) {
                tempLock?.releaseSafe()
                throw e.wrapIfNeed(uri)
            }
        }
    }

    override fun namesAt(fullDirPath: String, startsWith: String?): Set<UriAndName> {
        val dir = File(fullDirPath)
        val names = dir.listFiles().orEmpty()
            .filter { it.isFile }
            .mapTo(mutableSetOf()) {
                UriAndName(it.toContentUri(context), it.name)
            }
        return if (startsWith.isNullOrBlank()) {
            names
        } else {
            names.filter { it.name.startsWith(startsWith) }.toSet()
        }
    }

    companion object {

        private const val EXT_TEMP_FILE = "part"
    }
}