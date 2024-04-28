package net.maxsmr.mxstemplate.di.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.maxsmr.core.network.retrofit.interceptors.HostManager
import net.maxsmr.mxstemplate.manager.RadarIoHostManager
import javax.inject.Singleton

/**
 * Debug реализация. Проверять реализацию в release
 * Предоставляет [HostManager] в целевом аппе
 */
@[Module
InstallIn(SingletonComponent::class)]
class HostManagerModule {

    @[Provides Singleton net.maxsmr.core.di.RadarIoHostManager]
    fun provideRadarIoHostManager(): HostManager = RadarIoHostManager()
}