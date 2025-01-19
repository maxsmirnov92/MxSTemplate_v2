package net.maxsmr.vk_news_client.ui.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPost

@Parcelize
@Serializable
data class FeedPostUI(
    val id: Long,
    val communityId: Long,
    val communityName: String,
    val publicationTime: String,
    val contentText: String,
    val communityImageUrl: String,
    val contentImageUrl: String?,
    val statisticsItems: List<StatisticsUI>,
    val isFavourite: Boolean,
    val isLiked: Boolean
): Parcelable

fun FeedPost.toFeedPostUI(): FeedPostUI {
    return FeedPostUI(
        id = this.id,
        communityId = this.communityId,
        communityName = this.communityName,
        publicationTime = this.publicationTime,
        contentText = this.contentText,
        communityImageUrl = this.communityImageUrl,
        contentImageUrl = this.contentImageUrl,
        statisticsItems = this.statisticsItems.map { it.toStatisticsUI() },
        isFavourite = this.isFavourite,
        isLiked = this.isLiked,
    )
}

fun FeedPostUI.toFeedPost(): FeedPost {
    return FeedPost(
        id = this.id,
        communityId = this.communityId,
        communityName = this.communityName,
        publicationTime = this.publicationTime,
        contentText = this.contentText,
        communityImageUrl = this.communityImageUrl,
        contentImageUrl = this.contentImageUrl,
        statisticsItems = this.statisticsItems.map { it.toStatistics() },
        isFavourite = this.isFavourite,
        isLiked = this.isLiked,
    )
}