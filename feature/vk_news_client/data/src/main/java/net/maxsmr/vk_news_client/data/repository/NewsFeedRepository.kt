package net.maxsmr.vk_news_client.data.repository

import kotlinx.coroutines.flow.SharedFlow
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPost

interface NewsFeedRepository {

    val feedPostsUpdateEvents: SharedFlow<List<FeedPost>>

    suspend fun loadRecommendations() : List<FeedPost>

    suspend fun addLike(feedPost: FeedPost): Int

    suspend fun deleteLike(feedPost: FeedPost): Int
}