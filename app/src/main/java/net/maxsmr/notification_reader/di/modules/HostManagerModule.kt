package net.maxsmr.notification_reader.di.modules

import androidx.core.net.toUri
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import net.maxsmr.core.network.URL_SCHEME_HTTPS
import net.maxsmr.core.network.host.HostManager
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.notification_reader.BuildConfig
import net.maxsmr.notification_reader.manager.host.NotificationReaderHostManager
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
class HostManagerModule {

    @[Provides Singleton net.maxsmr.core.di.NotificationReaderHostManager]
    fun provideNotificationReaderHostManager(
        settingsRepo: SettingsDataStoreRepository
    ): HostManager =
        runBlocking {
            // динамическая базовая урла в зав-ти от настроек
            val uri = settingsRepo.getSettings().notificationsUrl.toUri()
            val defaultUri = BuildConfig.URL_NOTIFICATION_READER.toUri()
            NotificationReaderHostManager(
                uri.host?.takeIf { it.isNotEmpty() }
                    ?: defaultUri.host.orEmpty(),
                URL_SCHEME_HTTPS.equals(uri.scheme?.takeIf { it.isNotEmpty() }
                    ?: defaultUri.scheme,
                    true),
                uri.port.takeIf { it > 0 }
            )
        }
}