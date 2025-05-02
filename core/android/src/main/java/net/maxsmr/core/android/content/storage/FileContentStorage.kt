package net.maxsmr.core.android.content.storage

import android.content.Context
import android.net.Uri
import net.maxsmr.commonutils.media.toContentUri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Абстракция хранилища с использованием File API для доступа к ресурсам
 */
abstract class FileContentStorage(
    val context: Context,
) : ContentStorage<File> {

    override fun exists(name: String, path: String?): Result<Boolean> = runCatching {
        get(name, path).getOrThrow().exists()
    }

    override fun create(name: String, path: String?): Result<File> = runCatching {
        val file = get(name, path).getOrThrow()
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
        file
    }

    abstract override fun get(name: String, path: String?): Result<File>

    override fun write(resource: File, content: String): Result<Unit> =
        write(resource, content, false)

    override fun write(resource: File, content: InputStream): Result<Unit> =
        write(resource, content, false)

    override fun read(resource: File): Result<String> = runCatching {
        if (!resource.exists() || !resource.isFile) {
            throw IOException("Can't read from file $resource")
        }
        val bytes = ByteArray(resource.length().toInt())
        FileInputStream(resource).use { it.read(bytes) }
        String(bytes)
    }

    override fun read(resource: File, outputStream: OutputStream): Result<Unit> = runCatching {
        if (!resource.exists() || !resource.isFile) {
            throw IOException("Can't read from file $resource")
        }
        FileInputStream(resource).use {
            it.copyTo(outputStream)
        }
    }

    override fun delete(resource: File): Result<Boolean> = runCatching {
        if (resource.exists()) resource.delete() else true
    }

    override fun openInputStream(resource: File): Result<InputStream> = runCatching {
        FileInputStream(resource)
    }

    override fun openOutputStream(resource: File): Result<Pair<File, OutputStream>> = runCatching {
        Pair(resource, FileOutputStream(resource))
    }

    override fun shareUri(name: String, path: String?): Result<Uri?> = runCatching {
        get(name, path).getOrThrow().toContentUri(context)
    }

    override fun requiredPermissions(read: Boolean, write: Boolean): Array<String> = emptyArray()

    fun write(resource: File, content: String, append: Boolean): Result<Unit> = runCatching {
        FileOutputStream(resource, append).use {
            it.write(content.toByteArray())
            it.flush()
        }
    }

    fun write(resource: File, content: InputStream, append: Boolean): Result<Unit> = runCatching {
        FileOutputStream(resource, append).use {
            content.copyTo(it)
        }
    }

    /**
     * Возвращает целевую директорию файла
     *
     * @param root корневая директория этого хранилища
     * @param relativePath относительный путь **директории**, в которой находится файл
     */
    protected fun targetDir(root: File?, relativePath: String?): File? {
        if (relativePath.isNullOrBlank()) return root
        return runCatching<File> {
            val target = File(root, relativePath)
            if (!target.exists() || !target.isDirectory) {
                target.mkdirs()
            }
            target
        }.getOrNull() ?: root
    }

    enum class Type {

        PERSISTENT,
        CACHE
    }
}