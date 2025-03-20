package net.maxsmr.core.android.coroutines.execute.usecase.load

import android.content.Context
import net.maxsmr.core.android.content.storage.FileContentStorage
import net.maxsmr.core.android.content.storage.app_private.InternalFileStorage

import java.io.File
import java.io.FileOutputStream

/**
 * Базовый юзкейс для загрузки в Shared [ContentStorage]
 * с последующим копированием в [File] из [InternalFileStorage]
 * (например, с целью дальнейшего использования в PdfRender)
 */
abstract class BaseCombinedLoadFileUseCase(
    context: Context,
) : BaseSharedLoadFileUseCase<File>(context) {

    protected val internalStorage by lazy {
        InternalFileStorage(FileContentStorage.Type.PERSISTENT, context)
    }

    override suspend fun execute(parameters: FileParams): File {
        val (name, uri) = writeToUri(parameters)

        val internalFileResult = internalStorage.create(name)

        if (internalFileResult.isFailure) {
            throw internalFileResult.exceptionOrNull() ?: RuntimeException()
        }

        val file = internalFileResult.getOrThrow()

        contentResolver.openInputStream(uri)?.use {
            it.copyTo(FileOutputStream(file))
        }

        return file
    }
}