package net.maxsmr.mxstemplate.di.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.maxsmr.core.di.RadarIoRetrofit
import net.maxsmr.core.network.api.radar_io.AddressDataSource
import net.maxsmr.core.network.api.radar_io.RadarIoDataSource
import net.maxsmr.core.network.retrofit.client.RadarIoRetrofitClient
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
class DataSourceModule {

    @Provides
    @Singleton
    fun provideRadarIoDataSource(
        @RadarIoRetrofit retrofit: RadarIoRetrofitClient,
    ): AddressDataSource = RadarIoDataSource(retrofit)
}