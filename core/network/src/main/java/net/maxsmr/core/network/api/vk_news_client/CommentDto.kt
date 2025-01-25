package net.maxsmr.core.network.api.vk_news_client

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.core.network.retrofit.serializers.InstantAsSecondsSerializer

@Serializable
data class CommentDto(
    val id: Long,
    @SerialName("from_id")
    val authorId: Long,
    @Serializable(with = InstantAsSecondsSerializer::class)
    val date: Instant,
    val text: String,
)