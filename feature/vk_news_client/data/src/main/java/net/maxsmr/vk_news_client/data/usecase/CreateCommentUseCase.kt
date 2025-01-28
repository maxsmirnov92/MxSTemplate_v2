package net.maxsmr.vk_news_client.data.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import net.maxsmr.core.android.coroutines.execute.ExecuteResult
import net.maxsmr.core.android.coroutines.execute.usecase.FlowUseCase
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPostComment
import net.maxsmr.vk_news_client.data.repository.CommentsRepository

class CreateCommentUseCase(
    private val repository: CommentsRepository
): FlowUseCase<String, FeedPostComment>(Dispatchers.IO) {

    override fun execute(parameters: String): Flow<ExecuteResult<FeedPostComment>> = channelFlow {
        trySend(ExecuteResult.Loading)
        try {
            val id = repository.createComment(parameters)
            val result = repository.getComment(id)
            trySend(ExecuteResult.Success(result))
        } catch (e: Exception) {
            trySend(ExecuteResult.Error(e))
        }
    }
}