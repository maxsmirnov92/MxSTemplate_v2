package net.maxsmr.vk_news_client.ui.model

import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPostComment

data class FeedPostCommentUI(
    val id: Int,
    val authorName: String,
    val authorAvatarUrl: String,
    val commentText: String,
    val publicationDate: String
)

fun FeedPostComment.toFeedPostCommentUI(): FeedPostCommentUI {
    return FeedPostCommentUI(
        id = this.id,
        authorName = this.authorName,
        authorAvatarUrl = this.authorAvatarUrl,
        commentText = this.commentText,
        publicationDate = this.publicationDate,
    )
}