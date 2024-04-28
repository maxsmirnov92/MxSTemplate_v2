package net.maxsmr.core.android.content.storage

import android.content.Context
import android.net.Uri
import com.github.kittinunf.result.NoException
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getOrNull
import net.maxsmr.commonutils.media.toContentUri
import java.io.*

/**
 * Абстракция хранилища с использованием File API для доступа к ресурсам
 */
abstract class FileContentStorage(
    protected val storageType: Type,
) : ContentStorage<File> {

    override fun exists(name: String, path: String?, context: Context): Result<Boolean, Exception> = Result.of {
        get(name, path, context).get().exists()
    }

    override fun create(name: String, path: String?, context: Context): Result<File, Exception> = Result.of {
        val file = get(name, path, context).get()
        if (file.exists()) {
            file.delete()
        }
        file.createNewFile()
        file
    }

    abstract override fun get(name: String, path: String?, context: Context): Result<File, NoException>

    override fun write(resource: File, content: String): Result<Unit, Exception> = write(resource, content, false)

    fun write(resource: File, content: String, append: Boolean): Result<Unit, Exception> = Result.of {
        FileOutputStream(resource, append).use {
            it.write(content.toByteArray())
            it.flush()
        }
    }

    override fun read(resource: File): Result<String, Exception> = Result.of {
        if (!resource.exists() || !resource.isFile) {
            throw IOException("Can't read from file $resource")
        }
        val bytes = ByteArray(resource.length().toInt())
        FileInputStream(resource).use { it.read(bytes) }
        String(bytes)
    }

    override fun delete(resource: File, context: Context): Result<Boolean, Exception> = Result.of {
        if (resource.exists()) resource.delete() else true
    }

    override fun openInputStream(resource: File): Result<InputStream, Exception> = Result.of {
        FileInputStream(resource)
    }

    override fun openOutputStream(resource: File): Result<OutputStream, Exception> = Result.of {
        FileOutputStream(resource)
    }

    override fun shareUri(name: String, path: String?, context: Context): Result<Uri?, Exception> = Result.of {
        get(name, path, context).get().toContentUri(context)
    }

    override fun requiredPermissions(read: Boolean, write: Boolean): Array<String> = emptyArray()

    /**
     * Возвращает целевую директорию файла
     *
     * @param root корневая директория этого хранилища
     * @param relativePath относительный путь **директории**, в которой находится файл
     */
    protected fun targetDir(root: File?, relativePath: String?): File? {
        if (relativePath.isNullOrBlank()) return root
        return Result.of<File, Exception> {
            val target = File(root, relativePath)
            if (!target.exists() || !target.isDirectory) {
                target.mkdirs()
            }
            target
        }.getOrNull() ?: root
    }


    enum class Type {

        PERSISTENT,
        CACHE;
    }
}