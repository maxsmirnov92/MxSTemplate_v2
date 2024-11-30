package net.maxsmr.mxstemplate.di.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.maxsmr.mxstemplate.manager.host.DoubleGisRoutingHostManager
import net.maxsmr.core.network.host.HostManager
import net.maxsmr.mxstemplate.manager.host.RadarIoHostManager
import net.maxsmr.mxstemplate.manager.host.YandexGeocodeHostManager
import net.maxsmr.mxstemplate.manager.host.YandexSuggestHostManager
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
}