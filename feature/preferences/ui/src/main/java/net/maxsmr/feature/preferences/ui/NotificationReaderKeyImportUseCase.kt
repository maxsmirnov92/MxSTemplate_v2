package net.maxsmr.feature.preferences.ui

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import net.maxsmr.commonutils.media.openInputStreamOrThrow
import net.maxsmr.commonutils.stream.readStringOrThrow
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.android.coroutines.usecase.UseCase
import net.maxsmr.core.android.exceptions.EmptyResultException
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import javax.inject.Inject

class NotificationReaderKeyImportUseCase @Inject constructor(
    private val settingsRepo: SettingsDataStoreRepository
): UseCase<Uri, Unit>(Dispatchers.IO) {

    override suspend fun execute(parameters: Uri) {
        val stream = parameters.openInputStreamOrThrow(baseApplicationContext.contentResolver)
        val key = stream.readStringOrThrow()
        if (key.isEmpty()) {
            throw EmptyResultException(baseApplicationContext, false)
        }
        settingsRepo.getSettings().let {
            settingsRepo.updateSettings(it.copy(notificationsApiKey = key))
        }
    }
}