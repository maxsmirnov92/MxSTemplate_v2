package net.maxsmr.vk_news_client.ui.presentation.comments

import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPost
import net.maxsmr.vk_news_client.ui.model.FeedPostCommentUI
import net.maxsmr.vk_news_client.ui.model.FeedPostUI

sealed class CommentsScreenState {

    data object Initial: CommentsScreenState()

    data class Comments(val post: FeedPostUI, val comments: List<FeedPostCommentUI>) : CommentsScreenState()
}