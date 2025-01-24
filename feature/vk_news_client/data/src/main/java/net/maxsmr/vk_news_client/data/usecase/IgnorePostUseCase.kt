package net.maxsmr.vk_news_client.data.usecase

import kotlinx.coroutines.Dispatchers
import net.maxsmr.core.android.coroutines.execute.usecase.UseCase
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPost
import net.maxsmr.vk_news_client.data.repository.NewsFeedRepository
import javax.inject.Inject

class IgnorePostUseCase @Inject constructor(
    private val repository: NewsFeedRepository
) : UseCase<FeedPost, Unit>(Dispatchers.Default) {

    override suspend fun execute(parameters: FeedPost) {
        repository.delete(parameters)
    }
}