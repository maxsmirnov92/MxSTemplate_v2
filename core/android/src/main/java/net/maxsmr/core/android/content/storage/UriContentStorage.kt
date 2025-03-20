package net.maxsmr.core.android.content.storage

import android.content.ContentResolver
import android.net.Uri
import net.maxsmr.commonutils.media.exists
import net.maxsmr.core.utils.flatMap
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Абстракция хранилища с использованием content [Uri] для доступа к ресурсам
 */
abstract class UriContentStorage(
    protected val resolver: ContentResolver,
) : ContentStorage<Uri> {

    override fun exists(name: String, path: String?): Result<Boolean> {
        return get(name, path).map { it.exists(resolver) }
    }

    override fun write(resource: Uri, content: String): Result<Unit> = runCatching {
        openOutputStream(resource).getOrThrow().second.use {
            it.write(content.toByteArray())
            it.flush()
        }
    }

    override fun write(resource: Uri, content: InputStream): Result<Unit> = runCatching {
        openOutputStream(resource).getOrThrow().second.use {
            content.copyTo(it)
        }
    }

    override fun read(name: String, path: String?): Result<String> =
        get(name, path).flatMap { read(it) }

    override fun read(resource: Uri): Result<String> = runCatching {
        openInputStream(resource).getOrThrow().use {
            it.readBytes().let(::String)
        }
    }

    override fun read(resource: Uri, outputStream: OutputStream): Result<Unit> = runCatching {
        openInputStream(resource).getOrThrow().use {
            it.copyTo(outputStream)
        }
    }

    override fun delete(resource: Uri): Result<Boolean> = runCatching {
        resolver.delete(resource, null, null) > 0
    }

    override fun openInputStream(resource: Uri): Result<InputStream> = runCatching {
        resolver.openInputStream(resource) ?: throw IOException("Can't open stream from $resource")
    }

    override fun openOutputStream(resource: Uri): Result<Pair<Uri, OutputStream>> = runCatching {
        Pair(resource, resolver.openOutputStream(resource) ?: throw NullPointerException())
    }

    override fun shareUri(name: String, path: String?): Result<Uri?> =
        get(name, path)
}