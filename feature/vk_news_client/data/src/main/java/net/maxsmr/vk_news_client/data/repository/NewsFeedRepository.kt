package net.maxsmr.vk_news_client.data.repository

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPost
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPostComment
import net.maxsmr.core.domain.entities.feature.vk_news_client.Statistics

interface NewsFeedRepository {

    val feedPosts: StateFlow<List<FeedPost>>

    /**
     * Текущая порция загруженных данных + была ли загрузка с нуля или с определённого места
     */
    val feedPostsLastPage: SharedFlow<Pair<List<FeedPost>, Boolean>>

    val hasNextPage: Boolean

    suspend fun loadRecommendations() : List<FeedPost>

    suspend fun addLike(post: FeedPost): Int

    suspend fun deleteLike(post: FeedPost): Int

    suspend fun updateCount(id: Long, type: Statistics.StatsType)

    suspend fun delete(post: FeedPost)
}