package net.maxsmr.address_sorter.di.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.maxsmr.address_sorter.manager.host.DoubleGisRoutingHostManager
import net.maxsmr.address_sorter.manager.host.RadarIoHostManager
import net.maxsmr.address_sorter.manager.host.YandexGeocodeHostManager
import net.maxsmr.address_sorter.manager.host.YandexSuggestHostManager
import net.maxsmr.core.network.HostManager
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