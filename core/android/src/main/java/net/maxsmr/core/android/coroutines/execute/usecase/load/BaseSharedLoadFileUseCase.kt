package net.maxsmr.core.android.coroutines.execute.usecase.load

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import net.maxsmr.core.android.content.ContentType
import net.maxsmr.core.android.content.storage.ContentStorage
import net.maxsmr.core.android.content.storage.shared.SharedStorage
import net.maxsmr.core.android.coroutines.execute.usecase.load.BaseSharedLoadFileUseCase.FileParams

/**
 * Базовый юзкейс для загрузки данных в Shared [ContentStorage]
 */
abstract class BaseSharedLoadFileUseCase<Result>(
    context: Context,
) : BaseLoadFileUseCase<FileParams, Result>(context) {

    protected val sharedStorage: ContentStorage<Uri> by lazy {
        ContentStorage.createUriStorage(
            ContentStorage.StorageType.SHARED,
            ContentType.DOCUMENT,
            context
        )
    }

    protected val contentResolver: ContentResolver by lazy {
        context.contentResolver
    }

    protected suspend fun writeToUri(parameters: FileParams): Pair<String, Uri> {
        val name = parameters.name()

        (sharedStorage as? SharedStorage)?.let {
            it.shouldDeleteBeforeCreate = parameters.shouldDeleteBeforeCreate
        }

        val sharedFileResult = sharedStorage.create(name)

        if (sharedFileResult.isFailure) {
            throw sharedFileResult.exceptionOrNull() ?: RuntimeException()
        }

        val uri = sharedFileResult.getOrThrow()

        contentResolver.openOutputStream(uri)?.use {
            writeTo(it)
        } ?: throw RuntimeException("Cannot open OutputStream for uri $uri")

        return name to uri
    }

    /**
     * @param shouldDeleteBeforeCreate false, если при записи в MediaStore
     * с тем же именем предполагаются новые файлы с номером в скобках в имени
     */
    data class FileParams(
        override val name: String,
        override val ext: String,
        val shouldDeleteBeforeCreate: Boolean = false
    ) : IFileParams
}