package net.maxsmr.core.android.content.storage

import android.content.Context
import android.net.Uri
import com.github.kittinunf.result.Result
import net.maxsmr.commonutils.media.toContentUri
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
) : UriContentStorage(context.contentResolver) {

    override fun create(name: String, path: String?, context: Context): Result<Uri, Exception> = Result.of {
        file.create(name, path, context).get().toUri(context)
    }

    override fun get(name: String, path: String?, context: Context): Result<Uri, Exception> = Result.of {
        file.get(name, path, context).get().toUri(context)
    }

    override fun getOrCreate(name: String, path: String?, context: Context): Result<Uri, Exception> = Result.of {
        file.getOrCreate(name, path, context).get().toUri(context)
    }

    override fun delete(name: String, path: String?, context: Context): Result<Boolean, Exception> {
        return file.delete(name, path, context)
    }

    override fun exists(name: String, path: String?, context: Context): Result<Boolean, Exception> {
        return file.exists(name, path, context)
    }

    override fun write(content: String, name: String, path: String?, context: Context): Result<Unit, Exception> {
        return file.write(content, name, path, context)
    }

    override fun read(name: String, path: String?, context: Context): Result<String, Exception> {
        return file.read(name, path, context)
    }

    override fun openInputStream(name: String, path: String?, context: Context): Result<InputStream, Exception> {
        return file.openInputStream(name, path, context)
    }

    override fun openOutputStream(name: String, path: String?, context: Context): Result<OutputStream, Exception> {
        return file.openOutputStream(name, path, context)
    }

    override fun shareUri(name: String, path: String?, context: Context): Result<Uri?, Exception> {
        return file.shareUri(name, path, context)
    }

    override fun requiredPermissions(read: Boolean, write: Boolean): Array<String> {
        return file.requiredPermissions(read, write)
    }

    private fun File.toUri(context: Context): Uri = if (contentUri) {
        toContentUri(context)
    } else {
        Uri.fromFile(this)
    }
}