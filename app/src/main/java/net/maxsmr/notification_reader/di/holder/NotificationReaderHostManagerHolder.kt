package net.maxsmr.notification_reader.di.holder

import androidx.core.net.toUri
import kotlinx.coroutines.runBlocking
import net.maxsmr.core.BaseKeyInstanceHolder
import net.maxsmr.core.network.URL_SCHEME_HTTPS
import net.maxsmr.core.network.host.HostManager
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.notification_reader.BuildConfig
import net.maxsmr.notification_reader.manager.host.NotificationReaderHostManager

class NotificationReaderHostManagerHolder(
    private val settingsRepo: SettingsDataStoreRepository,
) : BaseKeyInstanceHolder<String, HostManager>({ url ->
        val uri = url.toUri()
        NotificationReaderHostManager(
            uri.host.orEmpty(),
            URL_SCHEME_HTTPS.equals(
                uri.scheme,
                true
            ),
            uri.port.takeIf { it > 0 }
        )
}) {

    override val key: String
        get() = runBlocking {
            // динамическая базовая урла в зав-ти от настроек
            val settings = settingsRepo.getSettings()
            var url = settings.notificationsUrl
            if (url.isEmpty()) {
                url = BuildConfig.URL_NOTIFICATION_READER
                if (url.isNotEmpty()) {
                    settingsRepo.updateSettings(settings.copy(notificationsUrl = url))
                }
            }
            return@runBlocking url
        }
}
