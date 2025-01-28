package net.maxsmr.vk_news_client.ui.presentation.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.gui.message.formatMessage
import net.maxsmr.commonutils.live.pgnErrorLoad
import net.maxsmr.commonutils.live.pgnLoading
import net.maxsmr.commonutils.live.pgnSuccessLoad
import net.maxsmr.commonutils.states.PgnLoadState
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.coroutines.execute.ExecuteResult
import net.maxsmr.core.domain.entities.feature.vk_news_client.Statistics
import net.maxsmr.vk_news_client.data.repository.NewsFeedRepository
import net.maxsmr.vk_news_client.data.usecase.ChangeLikeStatusUseCase
import net.maxsmr.vk_news_client.data.usecase.IgnorePostUseCase
import net.maxsmr.vk_news_client.data.usecase.LoadRecommendationsUseCase
import net.maxsmr.vk_news_client.ui.model.FeedPostUI
import net.maxsmr.vk_news_client.ui.model.toFeedPost
import net.maxsmr.vk_news_client.ui.model.toFeedPostUI
import javax.inject.Inject

@HiltViewModel
class NewsFeedViewModel @Inject constructor(
    private val loadRecommendationsUseCase: LoadRecommendationsUseCase,
    private val changeLikeStatusUseCase: ChangeLikeStatusUseCase,
    private val ignorePostUseCase: IgnorePostUseCase,
    private val repo: NewsFeedRepository,
    state: SavedStateHandle,
) : BaseViewModel(state) {

    private val _screenState = MutableLiveData<PgnLoadState<List<FeedPostUI>>>(PgnLoadState.pgnInitial())

    val screenState = _screenState as LiveData<PgnLoadState<List<FeedPostUI>>>

    override fun onInitialized() {
        super.onInitialized()
        if (screenState.value?.wasLoaded != true) {
            reloadRecommendations()
        }
        viewModelScope.launch {
            repo.feedPostsLastPage.collectLatest {
                _screenState.pgnSuccessLoad(
                    repo.feedPosts.value.map { post ->
                        post.toFeedPostUI()
                    },
                    isComplete = !repo.hasNextPage,
                    setValue = false
                )
            }
        }
    }

    fun reloadRecommendations() {
        loadRecommendations(true)
    }

    fun loadNextRecommendations() {
        loadRecommendations(false)
    }

    fun changeLikeStatus(feedPost: FeedPostUI) {
        if (!checkStateSuccess()) {
            return
        }
        dialogQueue.toggle(
            true, DIALOG_TAG_PROGRESS,
            TextMessage(net.maxsmr.core.android.R.string.loading)
        )
        viewModelScope.launch {
            when (val result = changeLikeStatusUseCase(feedPost.toFeedPost())) {
                is ExecuteResult.Error -> {
                    showSnackbar(
                        result.errorMessage().formatMessage(
                            net.maxsmr.core.network.R.string.error_request_failed_format,
                            net.maxsmr.core.network.R.string.error_request_failed,
                        )
                    )
                }

                else -> {

                }
            }
            dialogQueue.toggle(false, DIALOG_TAG_PROGRESS)
        }
    }

    fun updateCount(id: Long, type: Statistics.StatsType) {
        if (!checkStateSuccess()) {
            return
        }
        viewModelScope.launch {
            repo.updateCount(id, type)
        }
    }

    fun delete(item: FeedPostUI) {
        if (!checkStateSuccess()) {
            return
        }
        dialogQueue.toggle(true, DIALOG_TAG_PROGRESS, TextMessage(net.maxsmr.core.android.R.string.loading))
        viewModelScope.launch {
            when (val result = ignorePostUseCase(item.toFeedPost())) {
                is ExecuteResult.Error -> {
                    showSnackbar(
                        result.errorMessage().formatMessage(
                            net.maxsmr.core.network.R.string.error_request_failed_format,
                            net.maxsmr.core.network.R.string.error_request_failed,
                        )
                    )
                }

                else -> {

                }
            }
            dialogQueue.toggle(false, DIALOG_TAG_PROGRESS)
        }
    }

    private fun checkStateSuccess(): Boolean {
        val currentState = screenState.value ?: return false
        return currentState.isSuccess
    }

    private fun checkStateNotLoading(): Boolean {
        val currentState = screenState.value ?: return false
        return !currentState.isLoading
    }

    private fun loadRecommendations(shouldReload: Boolean) {
        if (!checkStateNotLoading()) {
            return
        }
        viewModelScope.launch {
            loadRecommendationsUseCase(shouldReload).collect {
                when (it) {
                    is ExecuteResult.Loading -> {
                        _screenState.pgnLoading(isFromStart = true, setValue = false)
                    }

                    is ExecuteResult.PgnLoading ->{
                        _screenState.pgnLoading(isFromStart = false, setValue = false)
                    }

                    is ExecuteResult.Error -> {
                        _screenState.pgnErrorLoad(it.errorData(), isComplete = !repo.hasNextPage, setValue = false)
                    }

                    else -> {
                        // при успехе ожидаем feedPostsLastPage
                    }
                }
            }
        }
    }
}