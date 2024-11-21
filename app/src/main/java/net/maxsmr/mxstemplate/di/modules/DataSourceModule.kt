package net.maxsmr.mxstemplate.di.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.maxsmr.core.di.DoubleGisRoutingRetrofit
import net.maxsmr.core.di.NotificationReaderRetrofit
import net.maxsmr.core.di.YandexGeocodeRetrofit
import net.maxsmr.core.di.YandexSuggestRetrofit
import net.maxsmr.core.network.api.BaseNotificationReaderDataSource
import net.maxsmr.core.network.api.DoubleGisRoutingDataSource
import net.maxsmr.core.network.api.GeocodeDataSource
import net.maxsmr.core.network.api.MockNotificationReaderDataSource
import net.maxsmr.core.network.api.RoutingDataSource
import net.maxsmr.core.network.api.SuggestDataSource
import net.maxsmr.core.network.api.YandexGeocodeDataSource
import net.maxsmr.core.network.api.YandexSuggestDataSource
import net.maxsmr.core.network.client.retrofit.CommonRetrofitClient
import net.maxsmr.core.network.client.retrofit.YandexGeocodeRetrofitClient
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
class DataSourceModule {

    @Provides
    @Singleton
    fun provideSuggestDataSource(
        @YandexSuggestRetrofit retrofit: CommonRetrofitClient,
    ): SuggestDataSource = YandexSuggestDataSource(retrofit)

    @Provides
    @Singleton
    fun provideGeocodeDataSource(
        @YandexGeocodeRetrofit retrofit: YandexGeocodeRetrofitClient,
    ): GeocodeDataSource = YandexGeocodeDataSource(retrofit)

    @Provides
    @Singleton
    fun provideRoutingDataSource(
        @DoubleGisRoutingRetrofit retrofit: CommonRetrofitClient,
    ): RoutingDataSource = DoubleGisRoutingDataSource(retrofit)

    @Provides
    @Singleton
    fun provideNotificationReaderDataSource(
        @NotificationReaderRetrofit retrofit: CommonRetrofitClient,
    ): BaseNotificationReaderDataSource = MockNotificationReaderDataSource()
}