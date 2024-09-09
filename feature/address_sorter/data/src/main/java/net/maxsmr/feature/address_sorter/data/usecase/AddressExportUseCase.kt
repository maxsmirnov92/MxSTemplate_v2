package net.maxsmr.feature.address_sorter.data.usecase

import android.net.Uri
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.maxsmr.commonutils.media.nameOrThrow
import net.maxsmr.commonutils.media.writeStringsOrThrow
import net.maxsmr.commonutils.text.appendExtension
import net.maxsmr.commonutils.text.removeExtension
import net.maxsmr.core.android.baseAppName
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.content.ContentType
import net.maxsmr.core.android.content.FileFormat
import net.maxsmr.core.android.content.storage.ContentStorage
import net.maxsmr.core.android.content.storage.shared.SharedStorage
import net.maxsmr.core.android.coroutines.usecase.UseCase
import net.maxsmr.core.di.BaseJson
import net.maxsmr.core.android.exceptions.EmptyResultException
import net.maxsmr.feature.address_sorter.data.repository.AddressRepo
import javax.inject.Inject

class AddressExportUseCase @Inject constructor(
    private val repository: AddressRepo,
    @BaseJson private val json: Json,
) : UseCase<String, String>(Dispatchers.IO) {

    private val storage: ContentStorage<Uri> by lazy {
        ContentStorage.createUriStorage(
            ContentStorage.StorageType.SHARED,
            ContentType.DOCUMENT,
            baseApplicationContext
        ).apply {
            if (this is SharedStorage) {
                shouldDeleteBeforeCreate = false
            }
        }
    }

    private val resolver by lazy {
        baseApplicationContext.contentResolver
    }

    override suspend fun execute(parameters: String): String {
        val items = repository.getItems().map { it.toDomain() }.filter { it.address.isNotEmpty() }
        if (items.isEmpty()) {
            throw EmptyResultException(baseApplicationContext, false)
        }

        val data = json.encodeToString(json.serializersModule.serializer(), items)

        val result = storage.create(
            (parameters.takeIf { it.isNotEmpty() } ?: EXPORT_FILE_NAME_DEFAULT)
                .removeExtension().appendExtension(FileFormat.JSON.extension),
            baseAppName
        )
        if (result is Result.Failure) {
            throw result.error
        } else {
            return result.getOrNull()?.let {
                it.writeStringsOrThrow(resolver, listOf(data))
                storage.path + it.nameOrThrow(resolver)
            } ?: throw EmptyResultException(baseApplicationContext, false)
        }
    }

    companion object {

        const val EXPORT_FILE_NAME_DEFAULT = "addresses"
    }
}