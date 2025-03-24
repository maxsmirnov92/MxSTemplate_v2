package net.maxsmr.core.android.coroutines.execute.usecase.base.load

import android.content.Context
import android.net.Uri

/**
 * Базовый юзкейс для загрузки данных в [Uri] от внешнего хранилища
 */
abstract class BaseUriSharedStorageLoadUseCase(
    context: Context
): BaseSharedStorageLoadUseCase<Uri>(context) {

    override suspend fun execute(parameters: FileParams): Uri {
        return writeToUri(parameters).second
    }
}