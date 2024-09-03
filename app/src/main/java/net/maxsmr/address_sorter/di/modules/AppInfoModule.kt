package net.maxsmr.address_sorter.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.maxsmr.address_sorter.BuildConfig
import net.maxsmr.commonutils.getSelfVersionCode
import net.maxsmr.commonutils.getSelfVersionName
import net.maxsmr.core.di.DI_NAME_VERSION_CODE
import net.maxsmr.core.di.DI_NAME_VERSION_NAME
import javax.inject.Named
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
class AppInfoModule {

    @[Provides Singleton Named(DI_NAME_VERSION_CODE)]
    fun provideVersionCode(@ApplicationContext context: Context): Int =
        context.getSelfVersionCode()?.toInt() ?: BuildConfig.VERSION_CODE

    @[Provides Singleton Named(DI_NAME_VERSION_NAME)]
    fun provideVersionName(@ApplicationContext context: Context): String =
        context.getSelfVersionName().takeIf { it.isNotEmpty() } ?: BuildConfig.VERSION_NAME
}