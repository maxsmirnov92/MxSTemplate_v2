package net.maxsmr.core.network.api.vk_news_client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CreateCommentResponseDto(
    @SerialName("comment_id")
    val commentId: Long,
    @SerialName("parents_stack")
    val parentStack: List<Long> = emptyList()
)