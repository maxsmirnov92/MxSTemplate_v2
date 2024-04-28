package net.maxsmr.mxstemplate.di.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.maxsmr.core.di.DI_NAME_VERSION_NAME
import net.maxsmr.mxstemplate.BuildConfig
import javax.inject.Named
import javax.inject.Singleton


@[Module
InstallIn(SingletonComponent::class)]
class AppInfoModule {

    @[Provides Singleton Named(DI_NAME_VERSION_NAME)]
    fun provideVersionName(): String = BuildConfig.VERSION_NAME
}