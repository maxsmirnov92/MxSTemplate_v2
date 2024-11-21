package net.maxsmr.notification_reader.di.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.maxsmr.core.di.NotificationReaderRetrofit
import net.maxsmr.core.network.api.BaseNotificationReaderDataSource
import net.maxsmr.core.network.api.MockNotificationReaderDataSource
import net.maxsmr.core.network.client.retrofit.CommonRetrofitClient
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
class DataSourceModule {

    @Provides
    @Singleton
    fun provideNotificationReaderDataSource(
        @NotificationReaderRetrofit retrofit: CommonRetrofitClient,
    ): BaseNotificationReaderDataSource = MockNotificationReaderDataSource()
}