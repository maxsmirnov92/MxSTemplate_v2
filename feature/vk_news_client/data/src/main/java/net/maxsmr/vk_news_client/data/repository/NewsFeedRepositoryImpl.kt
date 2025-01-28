package net.maxsmr.vk_news_client.data.repository

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPost
import net.maxsmr.core.domain.entities.feature.vk_news_client.Statistics
import net.maxsmr.core.domain.entities.feature.vk_news_client.Statistics.StatsType
import net.maxsmr.core.network.api.VkNewsDataSource

class NewsFeedRepositoryImpl(
    private val vkNewsDataSource: VkNewsDataSource,
) : NewsFeedRepository {

    override val feedPosts = MutableStateFlow<List<FeedPost>>(listOf())

    override val feedPostsLastPage = MutableSharedFlow<Pair<List<FeedPost>, Boolean>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val hasNextPage get() = recommendationsNextFrom != null

    private var recommendationsNextFrom: String? = null

    override suspend fun loadRecommendations(): List<FeedPost> {
        val isFromStart = !hasNextPage
//        if (!shouldReload && isFromStart) return feedPosts.value
        return vkNewsDataSource.getRecommended(recommendationsNextFrom).let {
            recommendationsNextFrom = it.second
            appendLastPage(it.first, isFromStart)
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
        appendLastPage(newList, true)
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
        appendLastPage(newList , true)
        try {
            vkNewsDataSource.ignorePost(post)
        } catch (e: Exception) {
            appendLastPage(currentList , true)
            throw e
        }
    }

    private suspend fun changeLikesCount(
        feedPost: FeedPost,
        count: Int,
        isLiked: Boolean,
    ) {
        appendLastPage(feedPosts.value.map {
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
        }, true)
    }

    private suspend fun appendLastPage(posts: List<FeedPost>, fromStart: Boolean) {
        if (fromStart) {
            feedPosts.value = posts
        } else {
            feedPosts.value += posts
        }
        feedPostsLastPage.emit(posts to fromStart)
    }
}