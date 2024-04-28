package net.maxsmr.core.android.content.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.mapEither
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Абстракция хранилища с использованием content [Uri] для доступа к ресурсам
 */
abstract class UriContentStorage(
    protected val resolver: ContentResolver,
) : ContentStorage<Uri> {

    override fun exists(name: String, path: String?, context: Context): Result<Boolean, Exception> {
        return get(name, path, context).mapEither({ true }, { it })
    }

    override fun write(resource: Uri, content: String): Result<Unit, Exception> = Result.of {
        openOutputStream(resource).get().apply {
            write(content.toByteArray())
            flush()
        }
    }

    override fun read(name: String, path: String?, context: Context): Result<String, Exception> =
        get(name, path, context)
            .flatMap { read(it) }

    override fun read(resource: Uri): Result<String, Exception> = Result.of {
        openInputStream(resource).get().use {
            it.readBytes().let(::String)
        }
    }

    override fun delete(resource: Uri, context: Context): Result<Boolean, Exception> = Result.of {
        resolver.delete(resource, null, null) > 0
    }

    override fun openInputStream(resource: Uri): Result<InputStream, Exception> = Result.of {
        resolver.openInputStream(resource) ?: throw IOException("Can't open stream from $resource")
    }

    override fun openOutputStream(resource: Uri): Result<OutputStream, Exception> = Result.of {
        resolver.openOutputStream(resource) ?: throw IOException("Can't open stream from $resource")
    }

    override fun shareUri(name: String, path: String?, context: Context): Result<Uri?, Exception> =
        get(name, path, context)
}