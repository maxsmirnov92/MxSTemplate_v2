package net.maxsmr.mxstemplate.di.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.maxsmr.core.di.SessionStorageType
import net.maxsmr.core.network.session.SessionStorage
import net.maxsmr.vk_news_client.data.VkSessionStorage
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
class SessionStorageModule {

    @[Provides Singleton net.maxsmr.core.di.SessionStorage(SessionStorageType.VK)]
    fun provideVkSessionStorage(
        @ApplicationContext context: Context,
    ): SessionStorage = VkSessionStorage()
}