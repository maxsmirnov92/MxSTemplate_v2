package net.maxsmr.core.network.api.vk_news_client

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.commonutils.format.formatDate
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPost
import net.maxsmr.core.domain.entities.feature.vk_news_client.Statistics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

@Serializable
data class GetRecommendedResponseDto(
    val items: List<ItemDto> = emptyList(),
    val groups: List<GroupDto> = emptyList(),
    @SerialName("next_from")
    val nextFrom: String? = null
) {

    fun asDomain(): List<FeedPost> {
        return items.filter { it.type == ItemDto.ItemType.POST }.mapNotNull { post ->
            val group = groups.find { group ->
                group.id == post.communityId.absoluteValue
            } ?: return@mapNotNull null

            fun List<List<ImageDto>>.lastUrl() =
                map { it.lastOrNull()?.url }.lastOrNull()?.takeIf { it.isNotEmpty() }

            FeedPost(
                id = post.id,
                communityId = post.communityId,
                communityName = group.name,
                publicationTime = formatDate(
                    Date(post.date.toEpochMilliseconds()),
                    SimpleDateFormat("d MMMM yyyy, hh:mm", Locale.getDefault())
                ),
                communityImageUrl = group.imageUrl,
                contentText = post.text,
                contentImageUrl = post.attachments
                    .mapNotNull { it.photo?.images }
                    .lastUrl() ?: post.attachments
                    .mapNotNull { it.video?.images }
                    .lastUrl(),
                statisticsItems = listOf(
                    Statistics(type = Statistics.StatsType.LIKES, post.likes.count),
                    Statistics(type = Statistics.StatsType.VIEWS, post.views.count),
                    Statistics(type = Statistics.StatsType.SHARES, post.reposts.count),
                    Statistics(type = Statistics.StatsType.COMMENTS, post.comments.count)
                ),
                isFavourite = post.isFavorite,
                isLiked = post.likes.userLikes == 1
            )
        }
    }
}