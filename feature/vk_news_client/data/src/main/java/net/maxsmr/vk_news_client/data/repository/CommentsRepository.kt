package net.maxsmr.vk_news_client.data.repository

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPostComment

interface CommentsRepository {

    val comments: StateFlow<List<FeedPostComment>>

    val lastComments: SharedFlow<List<FeedPostComment>>

    suspend fun loadComments(): List<FeedPostComment>

    suspend fun createComment(message: String): Long

    suspend fun getComment(commentId: Long): FeedPostComment
}