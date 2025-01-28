package net.maxsmr.core.network.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPost
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPostComment
import net.maxsmr.core.network.api.vk_news_client.GetCommentResponseDto
import net.maxsmr.core.network.api.vk_news_client.VkNewsDataService
import net.maxsmr.core.network.api.vk_news_client.exceptions.CreateCommentFailedException
import net.maxsmr.core.network.api.vk_news_client.exceptions.GetCommentFailedException
import net.maxsmr.core.network.api.vk_news_client.exceptions.IgnorePostFailedException
import net.maxsmr.core.network.client.retrofit.VkRetrofitClient
import kotlin.random.Random

interface VkNewsDataSource {

    suspend fun getRecommended(startFrom: String?, count: Int = 10): Pair<List<FeedPost>, String?>

    suspend fun ignorePost(feedPost: FeedPost)

    suspend fun addLike(feedPost: FeedPost): Int

    suspend fun deleteLike(feedPost: FeedPost): Int

    suspend fun getComments(feedPost: FeedPost, needLikes: Boolean = true): List<FeedPostComment>

    suspend fun createComment(feedPost: FeedPost, message: String): Long

    suspend fun getComment(feedPost: FeedPost, commentId: Long): FeedPostComment
}

class VkNewsDataSourceImpl(
    private val retrofit: VkRetrofitClient,
) : VkNewsDataSource {

    private val service by lazy {
        VkNewsDataService.instance(retrofit)
    }

    override suspend fun getRecommended(startFrom: String?, count: Int): Pair<List<FeedPost>, String?> =
        withContext(Dispatchers.IO) {
            val response = startFrom?.let { service.getRecommended(it, count) } ?: service.getRecommended(count)
            return@withContext response.asDomain() to response.nextFrom
        }

    override suspend fun ignorePost(feedPost: FeedPost) {
        withContext(Dispatchers.IO) {
            if (!service.ignorePost(feedPost.communityId, feedPost.id).status) {
                throw IgnorePostFailedException()
            }
        }
    }

    override suspend fun addLike(feedPost: FeedPost): Int = withContext(Dispatchers.IO) {
        service.addLikeForPost(feedPost.communityId, feedPost.id).count
    }

    override suspend fun deleteLike(feedPost: FeedPost): Int = withContext(Dispatchers.IO) {
        service.deleteLikeForPost(feedPost.communityId, feedPost.id).count
    }

    override suspend fun getComments(feedPost: FeedPost, needLikes: Boolean) = withContext(Dispatchers.IO) {
        service.getCommentsForPost(feedPost.communityId, feedPost.id, if (needLikes) 1 else 0).asDomain()
    }

    override suspend fun getComment(feedPost: FeedPost, commentId: Long) = withContext(Dispatchers.IO) {
        service.getComment(feedPost.communityId, feedPost.id, commentId).asDomain() ?: throw GetCommentFailedException()
    }

    override suspend fun createComment(feedPost: FeedPost, message: String): Long = withContext(Dispatchers.IO) {
        service.createComment(feedPost.communityId, feedPost.id, message)
            .commentId.takeIf { it > 0 } ?: throw CreateCommentFailedException()
    }
}