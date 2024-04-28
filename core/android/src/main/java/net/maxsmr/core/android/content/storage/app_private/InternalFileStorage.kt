package net.maxsmr.core.android.content.storage.app_private

import android.content.Context
import com.github.kittinunf.result.NoException
import com.github.kittinunf.result.Result
import net.maxsmr.core.android.content.storage.FileContentStorage
import java.io.File

/**
 * Внутреннее App-specific хранилище файлов приложения. Характеристики:
 *
 * 1. Объем хранилища - на некоторых девайсах может быть небольшой объем этого хранилища. Для хранения больших объемов
 * данных рекомендуется использовать [ExternalFileStorage].
 * 1. Разрешения на доступ - не требуются.
 * 1. Приватность данных - данные доступны только этому приложению, скрыты от остальных приложений и пользователя.
 * 1. Доступность хранилища - всегда доступно.
 */
class InternalFileStorage(
    storageType: Type,
) : FileContentStorage(storageType) {

    override fun get(name: String, path: String?, context: Context): Result<File, NoException> = Result.success(
        File(targetDir(storageType.rootDir(context), path), name)
    )

    private fun Type.rootDir(context: Context): File = when (this) {
        Type.PERSISTENT -> context.filesDir
        Type.CACHE -> context.cacheDir
    }
}