package net.maxsmr.core.android.coroutines.execute.usecase.load

import android.content.Context
import android.net.Uri

/**
 * Базовый юзкейс для загрузки данных в [Uri] от внешнего хранилища
 */
abstract class BaseUriSharedLoadFileUseCase(
    context: Context
): BaseSharedLoadFileUseCase<Uri>(context) {

    override suspend fun execute(parameters: FileParams): Uri {
        return writeToUri(parameters).second
    }
}