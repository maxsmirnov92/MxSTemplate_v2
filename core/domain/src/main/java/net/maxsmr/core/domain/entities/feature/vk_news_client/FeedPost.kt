package net.maxsmr.core.domain.entities.feature.vk_news_client

data class FeedPost(
    val id: Long,
    val communityId: Long,
    val communityName: String,
    val publicationTime: String,
    val contentText: String,
    val communityImageUrl: String,
    val contentImageUrl: String?,
    val statisticsItems: List<Statistics>,
    val isFavourite: Boolean,
    val isLiked: Boolean
)