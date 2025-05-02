package net.maxsmr.core.android.content.storage.app_private

import android.content.Context
import net.maxsmr.core.android.content.storage.FileContentStorage
import java.io.File

/**
 * Внутреннее App-specific хранилище файлов приложения. Характеристики:
 *
 * Разрешения на доступ - не требуются.
 * Приватность данных - данные доступны только этому приложению, скрыты от остальных приложений и пользователя.
 * Доступность хранилища - всегда доступно.
 */
class InternalFileStorage(
    storageType: Type,
    context: Context,
) : FileContentStorage(context) {

    val rootDir: File = when (storageType) {
        Type.PERSISTENT -> context.filesDir
        Type.CACHE -> context.cacheDir
    }

    override val path: String = rootDir.absolutePath

    override fun get(name: String, path: String?): Result<File> = Result.success(
        File(targetDir(rootDir, path), name)
    )
}