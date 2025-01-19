package net.maxsmr.core.domain.entities.feature.vk_news_client

data class FeedPostComment(
    val id: Int,
    val authorName: String,
    val authorAvatarUrl: String,
    val commentText: String,
    val publicationDate: String
)