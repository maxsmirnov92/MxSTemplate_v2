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
            feedPostsUpdateEvents.collectLatest { (data, fromStart) ->
                if (fromStart) {
                    feedPosts.value = data
                } else {
                    feedPosts.value += data
                }
            }
        }
    }

    override val feedPosts = MutableStateFlow<List<FeedPost>>(listOf())

    override val feedPostsUpdateEvents = MutableSharedFlow<Pair<List<FeedPost>, Boolean>>()

    override val hasNextPage get() = recommendationsNextFrom != null

    private var recommendationsNextFrom: String? = null

    override suspend fun loadRecommendations(): List<FeedPost> {
        val isFromStart = !hasNextPage
//        if (!shouldReload && isFromStart) return feedPosts.value
        return vkNewsDataSource.getRecommended(recommendationsNextFrom).let {
            recommendationsNextFrom = it.second
            feedPostsUpdateEvents.emit(it.first to isFromStart)
            it.first
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

    override suspend fun updateCount(id: Long, type: StatsType) {
        val currentList = feedPosts.value
        val newList = currentList.map {
            if (it.id == id) {
                it.copy(statisticsItems = it.statisticsItems.map { statsItem ->
                    if (statsItem.type == type) {
                        statsItem.copy(count = statsItem.count + 1)
                    } else {
                        statsItem
                    }
                })
            } else {
                it
            }
        }
        feedPostsUpdateEvents.emit(newList to true)
    }

    override suspend fun delete(item: FeedPost) {
        val currentList = feedPosts.value
        val newList = currentList.mapNotNull {
            if (it.id == item.id) {
                null
            } else {
                it
            }
        }
        feedPostsUpdateEvents.emit(newList to true)
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
        } to true)
    }
}