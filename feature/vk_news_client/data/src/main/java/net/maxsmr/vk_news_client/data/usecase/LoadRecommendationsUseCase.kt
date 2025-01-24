package net.maxsmr.vk_news_client.data.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import net.maxsmr.core.android.coroutines.execute.ExecuteResult
import net.maxsmr.core.android.coroutines.execute.usecase.FlowUseCase
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPost
import net.maxsmr.vk_news_client.data.repository.NewsFeedRepository
import javax.inject.Inject

class LoadRecommendationsUseCase @Inject constructor(
    private val repository: NewsFeedRepository
) : FlowUseCase<Boolean, List<FeedPost>>(Dispatchers.Default) {

    override fun execute(parameters: Boolean): Flow<ExecuteResult<List<FeedPost>>> = channelFlow {
        val hasNextPage = repository.hasNextPage
        if (!parameters && !hasNextPage) {
            trySend(ExecuteResult.Success(emptyList()))
            return@channelFlow
        }
        trySend(
            if (hasNextPage) {
                ExecuteResult.PgnLoading
            } else {
                ExecuteResult.Loading
            }
        )
        try {
            val result = repository.loadRecommendations()
            trySend(ExecuteResult.Success(result))
        } catch (e: Exception) {
            trySend(ExecuteResult.Error(e))
        }
    }
}