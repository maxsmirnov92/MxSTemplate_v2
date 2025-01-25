package net.maxsmr.vk_news_client.ui.presentation.comments

import net.maxsmr.vk_news_client.ui.model.FeedPostCommentUI
import net.maxsmr.vk_news_client.ui.model.FeedPostUI

data class CommentsScreenData(
    val post: FeedPostUI,
    val comments: List<FeedPostCommentUI>
) {

    val isEmpty = comments.isEmpty()
}