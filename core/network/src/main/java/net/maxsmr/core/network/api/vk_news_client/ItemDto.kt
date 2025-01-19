package net.maxsmr.core.network.api.vk_news_client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.core.network.retrofit.serializers.InstantAsSecondsSerializer

@Serializable
data class ItemDto(
    val id: Long,
    @SerialName("source_id")
    val communityId: Long,
    val type: ItemType?,
    @SerialName("is_favorite")
    val isFavorite: Boolean,
    val text: String,
    @Serializable(with = InstantAsSecondsSerializer::class)
    val date: kotlinx.datetime.Instant,
    val likes: LikesDto,
    val comments: CommentsDto,
    val views: ViewsDto,
    val reposts: RepostsDto,
    val attachments: List<AttachmentDto> = emptyList(),
) {

    enum class ItemType {
        @SerialName("post")
        POST
    }
}
