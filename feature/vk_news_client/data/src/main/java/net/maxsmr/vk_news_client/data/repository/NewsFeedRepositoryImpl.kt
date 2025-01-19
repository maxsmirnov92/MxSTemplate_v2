package net.maxsmr.vk_news_client.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPost
import net.maxsmr.core.domain.entities.feature.vk_news_client.Statistics
import net.maxsmr.core.domain.entities.feature.vk_news_client.Statistics.StatsType
import net.maxsmr.core.network.api.VkNewsDataSource

class NewsFeedRepositoryImpl(
    private val vkNewsDataSource: VkNewsDataSource,
) : NewsFeedRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        scope.launch {
            feedPostsUpdateEvents.collectLatest {
                feedPosts.value = it
            }
        }
    }

    override val feedPostsUpdateEvents = MutableSharedFlow<List<FeedPost>>()

    private val feedPosts = MutableStateFlow<List<FeedPost>>(listOf())

    override suspend fun loadRecommendations(): List<FeedPost> {
        return vkNewsDataSource.getRecommended().apply {
            feedPostsUpdateEvents.emit(this)
        }
    }

    override suspend fun addLike(feedPost: FeedPost): Int {
        return vkNewsDataSource.addLike(feedPost).apply {
            changeLikesCount(feedPost, this, true)
        }
    }

    override suspend fun deleteLike(feedPost: FeedPost): Int {
        return vkNewsDataSource.deleteLike(feedPost).apply {
            changeLikesCount(feedPost, this, false)
        }
    }

    private suspend fun changeLikesCount(
        feedPost: FeedPost,
        count: Int,
        isLiked: Boolean,
    ) {
        feedPostsUpdateEvents.emit(feedPosts.value.map {
            if (it.id == feedPost.id) {
                feedPost.copy(
                    statisticsItems = it.statisticsItems.map { stats ->
                        if (stats.type == StatsType.LIKES) {
                            Statistics(
                                StatsType.LIKES,
                                count,
                            )
                        } else {
                            stats
                        }
                    },
                    isLiked = isLiked
                )
            } else {
                it
            }
        })
    }
}