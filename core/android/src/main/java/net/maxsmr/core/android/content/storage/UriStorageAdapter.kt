package net.maxsmr.core.android.content.storage

import android.content.Context
import android.net.Uri
import net.maxsmr.commonutils.media.toContentUri
import net.maxsmr.commonutils.media.toFileUri
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Адаптер FileContentStorage -> UriContentStorage
 *
 * @param fileStorage исходное хранилище
 * @param contentUri true, если на выходе нужно получить content схему. false, если file
 */
class UriStorageAdapter(
    private val fileStorage: FileContentStorage,
    private val contentUri: Boolean = true,
    protected val context: Context,
) : UriContentStorage(context.contentResolver) {

    override val path: String = fileStorage.path

    override fun create(name: String, path: String?): Result<Uri> = runCatching {
        fileStorage.create(name, path).getOrThrow().toUri(context)
    }

    override fun get(name: String, path: String?): Result<Uri> = runCatching {
        fileStorage.get(name, path).getOrThrow().toUri(context)
    }

    override fun getOrCreate(name: String, path: String?): Result<Uri> = runCatching {
        fileStorage.getOrCreate(name, path).getOrThrow().toUri(context)
    }

    override fun delete(name: String, path: String?): Result<Boolean> {
        return fileStorage.delete(name, path)
    }

    override fun exists(name: String, path: String?): Result<Boolean> {
        return fileStorage.exists(name, path)
    }

    override fun write(content: String, name: String, path: String?): Result<Unit> {
        return fileStorage.write(content, name, path)
    }

    override fun read(name: String, path: String?): Result<String> {
        return fileStorage.read(name, path)
    }

    override fun openInputStream(name: String, path: String?): Result<InputStream> {
        return fileStorage.openInputStream(name, path)
    }

    override fun openOutputStream(name: String, path: String?): Result<Pair<Uri, OutputStream>> {
        return runCatching {
            val result = fileStorage.openOutputStream(name, path).getOrThrow()
            Pair(result.first.toUri(context), result.second)
        }
    }

    override fun shareUri(name: String, path: String?): Result<Uri?> {
        return fileStorage.shareUri(name, path)
    }

    override fun requiredPermissions(read: Boolean, write: Boolean): Array<String> {
        return fileStorage.requiredPermissions(read, write)
    }

    private fun File.toUri(context: Context): Uri = if (contentUri) {
        toContentUri(context)
    } else {
        toFileUri()
    }
}