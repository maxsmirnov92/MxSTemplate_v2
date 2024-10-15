package net.maxsmr.core.android.content.storage

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.mapEither
import net.maxsmr.commonutils.media.openResolverOutputStreamOrThrow
import net.maxsmr.commonutils.stream.copyStreamOrThrow
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Абстракция хранилища с использованием content [Uri] для доступа к ресурсам
 */
abstract class UriContentStorage(
    override val context: Context,
) : ContentStorage<Uri> {

    protected val resolver: ContentResolver by lazy { context.contentResolver }

    override fun exists(name: String, path: String?): Result<Boolean, Exception> {
        return get(name, path).mapEither({ true }, { it })
    }

    override fun write(resource: Uri, content: String): Result<Unit, Exception> = Result.of {
        openOutputStream(resource).get().use {
            it.write(content.toByteArray())
            it.flush()
        }
    }

    override fun write(resource: Uri, content: InputStream): Result<Unit, Exception> = Result.of {
        content.copyStreamOrThrow(openOutputStream(resource).get(), closeInput = false, closeOutput = true)
    }

    override fun read(name: String, path: String?): Result<String, Exception> =
        get(name, path).flatMap { read(it) }

    override fun read(resource: Uri): Result<String, Exception> = Result.of {
        openInputStream(resource).get().use {
            it.readBytes().let(::String)
        }
    }

    override fun read(resource: Uri, outputStream: OutputStream): Result<Unit, Exception> = Result.of {
        openInputStream(resource).get().copyStreamOrThrow(outputStream, closeInput = true, closeOutput = false)
    }

    override fun delete(resource: Uri): Result<Boolean, Exception> = Result.of {
        resolver.delete(resource, null, null) > 0
    }

    override fun openInputStream(resource: Uri): Result<InputStream, Exception> = Result.of {
        resolver.openInputStream(resource) ?: throw IOException("Can't open stream from $resource")
    }

    override fun openOutputStream(resource: Uri): Result<OutputStream, Exception> = Result.of {
        resource.openResolverOutputStreamOrThrow(resolver)
    }

    override fun shareUri(name: String, path: String?): Result<Uri?, Exception> =
        get(name, path)
}