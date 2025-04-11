package net.maxsmr.feature.address_sorter.data.usecase

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.maxsmr.commonutils.media.nameOrThrow
import net.maxsmr.commonutils.media.writeStringsOrThrow
import net.maxsmr.commonutils.text.appendExtension
import net.maxsmr.core.android.content.ContentType
import net.maxsmr.core.android.content.FileFormat
import net.maxsmr.core.android.content.storage.ContentStorage
import net.maxsmr.core.android.content.storage.shared.SharedStorage
import net.maxsmr.core.android.coroutines.execute.usecase.UseCase
import net.maxsmr.core.android.exceptions.EmptyResultException
import net.maxsmr.core.di.BaseJson
import net.maxsmr.core.di.DI_NAME_APP_NAME
import net.maxsmr.feature.address_sorter.data.repository.AddressRepo
import javax.inject.Inject
import javax.inject.Named

class AddressExportUseCase @Inject constructor(
    private val repository: AddressRepo,
    @BaseJson private val json: Json,
    @ApplicationContext private val context: Context,
) : UseCase<String, String>(Dispatchers.IO) {

    private val storage: ContentStorage<Uri> by lazy {
        ContentStorage.createUriStorage(
            ContentStorage.StorageType.SHARED,
            ContentType.DOCUMENT,
            context,
        ).apply {
            if (this is SharedStorage) {
                shouldDeleteBeforeCreate = false
            }
        }
    }

    private val resolver by lazy {
        context.contentResolver
    }

    override suspend fun execute(parameters: String): String {
        val items = repository.getAll().filter { it.address.isNotEmpty() }
        if (items.isEmpty()) {
            throw EmptyResultException(context, false)
        }

        val data = json.encodeToString(json.serializersModule.serializer(), items)

        val result = storage.create(
            (parameters.takeIf { it.isNotEmpty() } ?: EXPORT_FILE_NAME_DEFAULT)
                .appendExtension(FileFormat.JSON.extension)
        )
        if (result.isFailure) {
            throw result.exceptionOrNull() ?: RuntimeException()
        } else {
            return result.getOrNull()?.let {
                it.writeStringsOrThrow(resolver, listOf(data))
                storage.path + it.nameOrThrow(resolver)
            } ?: throw EmptyResultException(context, false)
        }
    }

    companion object {

        const val EXPORT_FILE_NAME_DEFAULT = "addresses"
    }
}