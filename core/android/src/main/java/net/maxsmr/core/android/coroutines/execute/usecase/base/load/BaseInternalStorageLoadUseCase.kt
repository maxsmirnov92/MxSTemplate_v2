package net.maxsmr.core.android.coroutines.execute.usecase.base.load

import android.content.Context
import net.maxsmr.core.android.content.storage.FileContentStorage
import net.maxsmr.core.android.content.storage.app_private.InternalFileStorage
import java.io.File

/*
* Базовый юзкейс для загрузки данных в [File] из [InternalFileStorage]
*/
abstract class BaseInternalStorageLoadUseCase(
    private val context: Context
): BaseFileLoadUseCase<BaseInternalStorageLoadUseCase.FileParams, File>() {

    protected val internalStorage by lazy {
        InternalFileStorage(FileContentStorage.Type.PERSISTENT, context)
    }

    override suspend fun execute(parameters: FileParams): File {
        val name = parameters.name()

        val internalFileResult = internalStorage.create(name)

        if (internalFileResult.isFailure) {
            throw internalFileResult.exceptionOrNull() ?: RuntimeException()
        }

        val file = internalFileResult.getOrThrow()

        file.outputStream().use {
            writeTo(it)
        }

        return file
    }

    data class FileParams(
        override val name: String,
        override val ext: String
    ): IFileParams
}