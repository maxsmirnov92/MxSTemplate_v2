package net.maxsmr.core.network.api.vk_news_client

import net.maxsmr.core.network.client.okhttp.interceptors.Authorization
import net.maxsmr.core.network.client.retrofit.VkRetrofitClient
import retrofit2.http.GET
import retrofit2.http.Query

internal interface VkNewsDataService {

    @Authorization
    @GET("method/newsfeed.getRecommended")
    suspend fun getRecommended(): GetRecommendedResponseDto

    @Authorization
    @GET("method/likes.add?type=post")
    suspend fun addLikeForPost(
        @Query("owner_id") ownerId: Long,
        @Query("item_id") postId: Long
    ): LikesCountResponseDto

    @Authorization
    @GET("method/likes.delete?type=post")
    suspend fun deleteLikeForPost(
        @Query("owner_id") ownerId: Long,
        @Query("item_id") postId: Long
    ): LikesCountResponseDto

    companion object {

        @Volatile
        private var instance: VkNewsDataService? = null

        @JvmStatic
        fun instance(client: VkRetrofitClient): VkNewsDataService =
            instance ?: synchronized(this) {
                instance ?: client.create(VkNewsDataService::class.java).also { instance = it }
            }
    }
}