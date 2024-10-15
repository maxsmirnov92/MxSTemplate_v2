package net.maxsmr.feature.address_sorter.data.usecase

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import net.maxsmr.commonutils.media.openInputStreamOrThrow
import net.maxsmr.commonutils.stream.readStringOrThrow
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.coroutines.usecase.UseCase
import net.maxsmr.core.database.model.address_sorter.AddressEntity.Companion.toEntity
import net.maxsmr.core.di.BaseJson
import net.maxsmr.core.domain.entities.feature.address_sorter.Address
import net.maxsmr.core.android.exceptions.EmptyResultException
import net.maxsmr.feature.address_sorter.data.repository.AddressRepo
import javax.inject.Inject

class AddressImportUseCase @Inject constructor(
    private val repository: AddressRepo,
    @BaseJson private val json: Json,
) : UseCase<Uri, Unit>(Dispatchers.IO) {

    override suspend fun execute(parameters: Uri) {
        val stream = parameters.openInputStreamOrThrow(baseApplicationContext.contentResolver)

        val data = stream.readStringOrThrow()

        val items = json.decodeFromString<List<Address>>(data)
            .filter { it.address.isNotEmpty() }
            .mapIndexed { index, item -> item.toEntity(index) }
        if (items.isEmpty()) {
            throw EmptyResultException(baseApplicationContext, false)
        }

        repository.addItems(items)
    }
}