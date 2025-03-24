package net.maxsmr.core.android.coroutines.execute.usecase.base.load

import java.io.File

/**
 * Базовый юзкейс для загрузки данных в предоставленную папку
 */
abstract class BaseConcreteFileLoadUseCase() : BaseFileLoadUseCase<BaseConcreteFileLoadUseCase.FileParams, File>() {

    protected abstract val parentDir: File

    override suspend fun execute(parameters: FileParams): File {
        val name = parameters.name()

        val file = File(parentDir, name)
        if (file.exists()) {
            file.delete()
        }
        if (!file.createNewFile()) {
            throw RuntimeException("Create file failed")
        }

        file.outputStream().use {
            writeTo(it)
        }

        return file
    }

    data class FileParams(
        override val name: String,
        override val ext: String,
    ) : IFileParams
}