package net.maxsmr.core.network.api.vk_news_client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPostComment

@Serializable
data class GetCommentResponseDto(
    @SerialName("items")
    override val comments: List<CommentDto>,
    override val profiles: List<ProfileDto> = emptyList(),
    val likes: List<LikesDto> = emptyList(),
): BaseCommentsResponseDto {

    fun asDomain(): FeedPostComment? = comments.firstOrNull {
        it.text.isNotEmpty()
    }?.let {
        val author = profiles.firstOrNull { p -> p.id == it.authorId }
        FeedPostComment(
            it.id,
            author?.let {  "${author.firstName} ${author.lastName}" }.orEmpty(),
            author?.avatarUrl,
            it.text,
            it.date.toString()
        )
    }
}