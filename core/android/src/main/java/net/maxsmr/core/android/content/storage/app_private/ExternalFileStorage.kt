package net.maxsmr.core.android.content.storage.app_private

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.github.kittinunf.result.NoException
import com.github.kittinunf.result.Result
import net.maxsmr.core.android.content.ContentType
import net.maxsmr.core.android.content.rootDir
import net.maxsmr.core.android.content.storage.FileContentStorage
import java.io.File
import java.io.IOException

/**
 * Внещнее App-specific хранилище файлов приложения. Характеристики:
 *
 * 1. Объем хранилища - на большинстве девайсов объем хранилища больше, чем [InternalFileStorage].
 * 1. Разрешения на доступ - не требуются.
 * 1. Приватность данных:
 *      1. Scoped Storage enabled - данные доступны только этому приложению, скрыты от остальных приложений.
 *      1. Scoped Storage disabled (Android 9-, либо requestLegacyExternalStorage флаг в манифесте) -
 *      данные доступны другим приложениям.
 * 1. Доступность хранилища - может быть недоступно, например, если физически располагается на извлекаемой SD карте.
 */
class ExternalFileStorage internal constructor(
    private val rootDir: File?,
    context: Context,
) : FileContentStorage(context) {

    constructor(
        storageType: Type,
        contentType: ContentType,
        context: Context,
    ) : this(
        when (storageType) {
            Type.PERSISTENT -> context.getExternalFilesDir(contentType.rootDir)
            Type.CACHE -> context.externalCacheDir
        },
        context,
    )

    override val path: String = rootDir?.absolutePath.orEmpty()

    override fun get(name: String, path: String?): Result<File, NoException> = Result.success(
        File(targetDir(rootDir, path), name)
    )

    override fun getOrCreate(name: String, path: String?): Result<File, Exception> {
        if (!writable()) throw IOException("External storage not writable")
        return super.getOrCreate(name, path)
    }

    override fun write(content: String, name: String, path: String?): Result<Unit, Exception> {
        if (!writable()) throw IOException("External storage not writable")
        return super.write(content, name, path)
    }

    override fun write(resource: File, content: String): Result<Unit, Exception> {
        if (!writable()) throw IOException("External storage not writable")
        return super.write(resource, content)
    }

    override fun read(name: String, path: String?): Result<String, Exception> {
        if (!readable()) throw IOException("External storage not readable")
        return super.read(name, path)
    }

    override fun read(resource: File): Result<String, Exception> {
        if (!readable()) throw IOException("External storage not readable")
        return super.read(resource)
    }

    override fun delete(name: String, path: String?): Result<Boolean, Exception> {
        if (!writable()) throw IOException("External storage not writable")
        return super.delete(name, path)
    }

    override fun delete(resource: File): Result<Boolean, Exception> {
        if (!writable()) throw IOException("External storage not writable")
        return super.delete(resource)
    }

    override fun shareUri(name: String, path: String?): Result<Uri?, Exception> {
        if (!readable()) throw IOException("External storage not readable")
        return super.shareUri(name, path)
    }


    companion object {

        /**
         * @return true, если внешнее хранилище примонтировано и доступно для чтения
         */
        @JvmStatic
        fun readable(): Boolean = Environment.getExternalStorageState() in
                setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)

        /**
         * @return true, если внешнее хранилище примонтировано и доступно для записи
         */
        @JvmStatic
        fun writable(): Boolean =
            Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
}