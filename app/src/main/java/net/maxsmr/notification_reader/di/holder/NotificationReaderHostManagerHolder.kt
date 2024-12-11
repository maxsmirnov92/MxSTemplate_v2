package net.maxsmr.notification_reader.di.holder

import kotlinx.coroutines.runBlocking
import net.maxsmr.core.BaseKeyInstanceHolder
import net.maxsmr.core.network.host.UriHostManager
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.notification_reader.BuildConfig

class NotificationReaderHostManagerHolder(
    private val settingsRepo: SettingsDataStoreRepository,
) : BaseKeyInstanceHolder<String, UriHostManager>({ url ->
    UriHostManager(url)
}) {

    override val key: String
        get() = runBlocking {
            // динамическая базовая урла в зав-ти от настроек
            return@runBlocking settingsRepo.getSettings().notificationsUrl
                .takeIf { it.isNotEmpty() } ?: BuildConfig.URL_NOTIFICATIONS
        }
}