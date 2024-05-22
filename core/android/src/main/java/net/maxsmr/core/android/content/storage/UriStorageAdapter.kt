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
 * @param file исходное хранилище
 * @param contentUri true, если на выходе нужно получить content схему. false, если file
 */
class UriStorageAdapter(
    private val file: FileContentStorage,
    private val contentUri: Boolean = true,
    context: Context,
) : UriContentStorage(context) {

    override val path: String = file.path

    override fun create(name: String, path: String?): Result<Uri, Exception> = Result.of {
        file.create(name, path).get().toUri(context)
    }

    override fun get(name: String, path: String?): Result<Uri, Exception> = Result.of {
        file.get(name, path).get().toUri(context)
    }

    override fun getOrCreate(name: String, path: String?): Result<Uri, Exception> = Result.of {
        file.getOrCreate(name, path).get().toUri(context)
    }

    override fun delete(name: String, path: String?): Result<Boolean, Exception> {
        return file.delete(name, path)
    }

    override fun exists(name: String, path: String?): Result<Boolean, Exception> {
        return file.exists(name, path)
    }

    override fun write(content: String, name: String, path: String?): Result<Unit, Exception> {
        return file.write(content, name, path)
    }

    override fun read(name: String, path: String?): Result<String, Exception> {
        return file.read(name, path)
    }

    override fun openInputStream(name: String, path: String?): Result<InputStream, Exception> {
        return file.openInputStream(name, path)
    }

    override fun openOutputStream(name: String, path: String?): Result<OutputStream, Exception> {
        return file.openOutputStream(name, path)
    }

    override fun shareUri(name: String, path: String?): Result<Uri?, Exception> {
        return file.shareUri(name, path)
    }

    override fun requiredPermissions(read: Boolean, write: Boolean): Array<String> {
        return file.requiredPermissions(read, write)
    }

    private fun File.toUri(context: Context): Uri = if (contentUri) {
        toContentUri(context)
    } else {
        toFileUri()
    }
}