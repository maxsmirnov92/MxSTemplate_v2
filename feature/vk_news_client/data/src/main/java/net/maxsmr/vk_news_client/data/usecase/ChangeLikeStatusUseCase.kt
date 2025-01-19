package net.maxsmr.vk_news_client.data.usecase

import kotlinx.coroutines.Dispatchers
import net.maxsmr.core.android.coroutines.execute.usecase.UseCase
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPost
import net.maxsmr.vk_news_client.data.repository.NewsFeedRepository
import javax.inject.Inject

class ChangeLikeStatusUseCase @Inject constructor(
    private val repository: NewsFeedRepository,
) : UseCase<FeedPost, Int>(Dispatchers.Default) {

    override suspend fun execute(parameters: FeedPost): Int {
        return if (parameters.isLiked) {
            repository.deleteLike(parameters)
        } else {
            repository.addLike(parameters)
        }
    }
}