package net.maxsmr.core.network.api.vk_news_client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LikesDto(
    val count: Int,
    @SerialName("user_likes")
    val userLikes: Int
)
