package net.maxsmr.core.android.content.storage

import android.content.Context
import android.net.Uri
import com.github.kittinunf.result.Result
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
    context: Context,
) : UriContentStorage(context) {

    override val path: String = fileStorage.path

    override fun create(name: String, path: String?): Result<Uri, Exception> = Result.of {
        fileStorage.create(name, path).get().toUri(context)
    }

    override fun get(name: String, path: String?): Result<Uri, Exception> = Result.of {
        fileStorage.get(name, path).get().toUri(context)
    }

    override fun getOrCreate(name: String, path: String?): Result<Uri, Exception> = Result.of {
        fileStorage.getOrCreate(name, path).get().toUri(context)
    }

    override fun delete(name: String, path: String?): Result<Boolean, Exception> {
        return fileStorage.delete(name, path)
    }

    override fun exists(name: String, path: String?): Result<Boolean, Exception> {
        return fileStorage.exists(name, path)
    }

    override fun write(content: String, name: String, path: String?): Result<Unit, Exception> {
        return fileStorage.write(content, name, path)
    }

    override fun read(name: String, path: String?): Result<String, Exception> {
        return fileStorage.read(name, path)
    }

    override fun openInputStream(name: String, path: String?): Result<InputStream, Exception> {
        return fileStorage.openInputStream(name, path)
    }

    override fun openOutputStream(name: String, path: String?): Result<Pair<Uri, OutputStream>, Exception> {
        return Result.of {
            val result = fileStorage.openOutputStream(name, path).get()
            Pair(result.first.toUri(context), result.second)
        }
    }

    override fun shareUri(name: String, path: String?): Result<Uri?, Exception> {
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