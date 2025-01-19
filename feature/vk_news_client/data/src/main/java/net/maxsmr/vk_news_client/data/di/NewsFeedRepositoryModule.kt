package net.maxsmr.vk_news_client.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.maxsmr.core.network.api.VkNewsDataSource
import net.maxsmr.vk_news_client.data.repository.NewsFeedRepository
import net.maxsmr.vk_news_client.data.repository.NewsFeedRepositoryImpl
import javax.inject.Singleton

@[Module
InstallIn(SingletonComponent::class)]
object NewsFeedRepositoryModule {

    @[Provides Singleton]
    fun providesNewsFeedRepository(vkNewsDataSource: VkNewsDataSource): NewsFeedRepository {
        return NewsFeedRepositoryImpl(vkNewsDataSource)
    }
}