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
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPostComment
import net.maxsmr.core.domain.entities.feature.vk_news_client.Statistics
import net.maxsmr.core.domain.entities.feature.vk_news_client.Statistics.StatsType
import net.maxsmr.core.network.api.VkNewsDataSource
import net.maxsmr.core.network.api.vk_news_client.CommentsDto

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

    override suspend fun addLike(post: FeedPost): Int {
        return vkNewsDataSource.addLike(post).apply {
            changeLikesCount(post, this, true)
        }
    }

    override suspend fun deleteLike(post: FeedPost): Int {
        return vkNewsDataSource.deleteLike(post).apply {
            changeLikesCount(post, this, false)
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

    override suspend fun delete(post: FeedPost) {
        val currentList = feedPosts.value
        val newList = currentList.mapNotNull {
            if (it.id == post.id) {
                null
            } else {
                it
            }
        }
        // нужна рекомпозиция, т.к. элемент уже был изменён
        feedPostsUpdateEvents.emit(newList to true)
        try {
            vkNewsDataSource.ignorePost(post)
        } catch (e: Exception) {
            feedPostsUpdateEvents.emit(currentList to true)
            throw e
        }
    }

    override suspend fun loadComments(post: FeedPost): List<FeedPostComment> {
        return vkNewsDataSource.getComments(post)
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