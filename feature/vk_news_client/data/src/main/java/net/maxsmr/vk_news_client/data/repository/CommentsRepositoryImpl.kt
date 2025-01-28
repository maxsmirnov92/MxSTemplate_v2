package net.maxsmr.vk_news_client.data.repository

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPost
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPostComment
import net.maxsmr.core.network.api.VkNewsDataSource

/**
 * Репозиторий для комментариев с привязкой к конкретному посту
 */
class CommentsRepositoryImpl(
    private val vkNewsDataSource: VkNewsDataSource,
    private val post: FeedPost
): CommentsRepository {

    private val _comments = MutableStateFlow<List<FeedPostComment>>(listOf())

    override val comments = _comments.asStateFlow()

    private val _lastComments = MutableSharedFlow<List<FeedPostComment>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val lastComments = _lastComments.asSharedFlow()

    override suspend fun loadComments(): List<FeedPostComment> {
        _comments.value = listOf()
        return vkNewsDataSource.getComments(post).also {
            appendLastComments(it)
        }
    }

    override suspend fun createComment(message: String): Long {
        return vkNewsDataSource.createComment(post, message)
    }

    override suspend fun getComment(commentId: Long): FeedPostComment {
        return vkNewsDataSource.getComment(post, commentId).also {
            appendLastComments(listOf(it))
        }
    }

    private suspend fun appendLastComments(comments: List<FeedPostComment>) {
        _comments.value += comments
        _lastComments.emit(comments)
    }
}