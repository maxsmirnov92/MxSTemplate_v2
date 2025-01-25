package net.maxsmr.core.network.api.vk_news_client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPostComment

@Serializable
data class GetCommentsResponseDto(
    val count: Int,
    @SerialName("items")
    val comments: List<CommentDto>,
    @SerialName("current_level_count") val currentLevelCount: Int,
    @SerialName("can_post") val canPost: Boolean,
    @SerialName("show_reply_button") val showReplyButton: Boolean,
    @SerialName("group_can_post") val groupCanPost: Boolean? = null,
    val profiles: List<ProfileDto> = emptyList(),
    val likes: List<LikesDto> = emptyList(),
) {

    fun asDomain(): List<FeedPostComment> = comments.mapNotNull {
        if (it.text.isEmpty()) return@mapNotNull null
        val author = profiles.firstOrNull { p -> p.id == it.authorId } ?: return@mapNotNull null
        FeedPostComment(
            it.id,
            "${author.firstName} ${author.lastName}",
            author.avatarUrl,
            it.text,
            it.date.toString()
        )
    }
}