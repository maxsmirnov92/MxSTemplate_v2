package net.maxsmr.core.network.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPost
import net.maxsmr.core.domain.entities.feature.vk_news_client.Statistics
import net.maxsmr.core.network.api.vk_news_client.VkNewsDataService
import net.maxsmr.core.network.client.retrofit.VkRetrofitClient
import kotlin.random.Random

interface VkNewsDataSource {

    suspend fun getRecommended(startFrom: String?, count: Int = 10): Pair<List<FeedPost>, String?>

    suspend fun addLike(feedPost: FeedPost): Int

    suspend fun deleteLike(feedPost: FeedPost): Int
}

class VkNewsDataSourceImpl(
    private val retrofit: VkRetrofitClient
): VkNewsDataSource {

    private val service by lazy {
        VkNewsDataService.instance(retrofit)
    }

    override suspend fun getRecommended(startFrom: String?, count: Int): Pair<List<FeedPost>, String?> = withContext(Dispatchers.IO) {
        val response = startFrom?.let {  service.getRecommended(it, count) } ?: service.getRecommended(count)
        return@withContext response.asDomain() to response.nextFrom
    }

    override suspend fun addLike(feedPost: FeedPost) : Int = withContext(Dispatchers.IO) {
        service.addLikeForPost(feedPost.communityId, feedPost.id).count
    }

    override suspend fun deleteLike(feedPost: FeedPost): Int = withContext(Dispatchers.IO) {
        service.deleteLikeForPost(feedPost.communityId, feedPost.id).count
    }
}

class MockVkNewsDataSourceImpl: VkNewsDataSource {

    override suspend fun getRecommended(startFrom: String?, count: Int): Pair<List<FeedPost>, String?> {
        delay(3000)
        return listOf<FeedPost>() to null
    }

    override suspend fun addLike(feedPost: FeedPost): Int {
        delay(3000)
        return Random.nextInt(1000)
    }

    override suspend fun deleteLike(feedPost: FeedPost): Int {
        delay(3000)
        return Random.nextInt(1000)
    }
}