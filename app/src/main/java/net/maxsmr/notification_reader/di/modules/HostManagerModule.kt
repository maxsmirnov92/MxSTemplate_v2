package net.maxsmr.notification_reader.di.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.notification_reader.di.holder.NotificationReaderHostManagerHolder
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
class HostManagerModule {

    @[Provides Singleton net.maxsmr.core.di.NotificationReaderHostManager]
    fun provideNotificationReaderHostManager(
        settingsRepo: SettingsDataStoreRepository,
    ): NotificationReaderHostManagerHolder = NotificationReaderHostManagerHolder(settingsRepo)
}