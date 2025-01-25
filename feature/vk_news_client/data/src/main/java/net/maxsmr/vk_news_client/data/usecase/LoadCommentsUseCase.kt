package net.maxsmr.vk_news_client.data.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import net.maxsmr.core.android.coroutines.execute.ExecuteResult
import net.maxsmr.core.android.coroutines.execute.usecase.FlowUseCase
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPost
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPostComment
import net.maxsmr.vk_news_client.data.repository.NewsFeedRepository
import javax.inject.Inject

class LoadCommentsUseCase @Inject constructor(
    private val repository: NewsFeedRepository,
) : FlowUseCase<FeedPost, List<FeedPostComment>>(Dispatchers.Default) {

    override fun execute(parameters: FeedPost): Flow<ExecuteResult<List<FeedPostComment>>> = channelFlow {
        trySend(ExecuteResult.Loading)
        try {
            val result = repository.loadComments(parameters)
            trySend(ExecuteResult.Success(result))
        } catch (e: Exception) {
            trySend(ExecuteResult.Error(e))
        }
    }
}