package net.maxsmr.mxstemplate.di.modules

import androidx.core.net.toUri
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import net.maxsmr.core.android.network.URL_SCHEME_HTTPS
import net.maxsmr.mxstemplate.manager.host.DoubleGisRoutingHostManager
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.mxstemplate.BuildConfig
import net.maxsmr.mxstemplate.manager.host.NotificationReaderHostManager
import net.maxsmr.core.network.host.HostManager
import net.maxsmr.mxstemplate.manager.host.RadarIoHostManager
import net.maxsmr.mxstemplate.manager.host.YandexGeocodeHostManager
import net.maxsmr.mxstemplate.manager.host.YandexSuggestHostManager
import javax.inject.Provider
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
class HostManagerModule {

    @[Provides Singleton net.maxsmr.core.di.RadarIoHostManager]
    fun provideRadarIoHostManager(): HostManager = RadarIoHostManager()

    @[Provides Singleton net.maxsmr.core.di.YandexSuggestHostManager]
    fun provideYandexSuggestHostManager(): HostManager = YandexSuggestHostManager()

    @[Provides Singleton net.maxsmr.core.di.YandexGeocodeHostManager]
    fun provideYandexGeocodeHostManager(): HostManager = YandexGeocodeHostManager()

    @[Provides Singleton net.maxsmr.core.di.DoubleGisRoutingHostManager]
    fun provideDoubleGisRoutingHostManager(): HostManager = DoubleGisRoutingHostManager()

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