package net.maxsmr.mxstemplate.di.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.maxsmr.core.di.YandexSuggestRetrofit
import net.maxsmr.core.network.api.AddressDataSource
import net.maxsmr.core.network.api.YandexSuggestDataSource
import net.maxsmr.core.network.retrofit.client.CommonRetrofitClient
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
class DataSourceModule {

    @Provides
    @Singleton
    fun provideDataSource(
        @YandexSuggestRetrofit retrofit: CommonRetrofitClient,
    ): AddressDataSource = YandexSuggestDataSource(retrofit)
}