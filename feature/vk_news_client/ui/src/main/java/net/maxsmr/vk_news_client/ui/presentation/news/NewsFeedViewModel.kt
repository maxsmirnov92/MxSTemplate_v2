package net.maxsmr.vk_news_client.ui.presentation.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.pgnSuccessLoad
import net.maxsmr.commonutils.states.PgnLoadState
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.coroutines.execute.ExecuteResult
import net.maxsmr.core.android.coroutines.execute.asPgnState
import net.maxsmr.core.android.coroutines.execute.mapData
import net.maxsmr.core.domain.entities.feature.vk_news_client.Statistics
import net.maxsmr.vk_news_client.data.repository.NewsFeedRepository
import net.maxsmr.vk_news_client.data.usecase.ChangeLikeStatusUseCase
import net.maxsmr.vk_news_client.data.usecase.LoadRecommendationsUseCase
import net.maxsmr.vk_news_client.ui.model.FeedPostUI
import net.maxsmr.vk_news_client.ui.model.toFeedPost
import net.maxsmr.vk_news_client.ui.model.toFeedPostUI
import javax.inject.Inject

@HiltViewModel
class NewsFeedViewModel @Inject constructor(
    private val loadRecommendationsUseCase: LoadRecommendationsUseCase,
    private val changeLikeStatusUseCase: ChangeLikeStatusUseCase,
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
            repo.feedPostsUpdateEvents.collectLatest {
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
        dialogQueue.toggle(true, DIALOG_TAG_PROGRESS) {
            setMessage(net.maxsmr.core.android.R.string.loading)
        }
        viewModelScope.launch {
            when (val result = changeLikeStatusUseCase(feedPost.toFeedPost())) {
                is ExecuteResult.Error -> {
                    showSnackbar(
                        TextMessage(
                            net.maxsmr.core.network.R.string.error_request_failed_format,
                            result.errorMessage()
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
        val currentState = screenState.value ?: return
        if (!currentState.isSuccess) return
        viewModelScope.launch {
            repo.updateCount(id, type)
        }
    }

    fun delete(item: FeedPostUI) {
        val currentState = screenState.value ?: return
        if (!currentState.isSuccess) return
        viewModelScope.launch {
            repo.delete(item.toFeedPost())
        }
    }

    private fun loadRecommendations(shouldReload: Boolean) {
        viewModelScope.launch {
            loadRecommendationsUseCase(shouldReload).collect {
                val mappedResult = it.mapData { data -> data.map { post -> post.toFeedPostUI() } }
                if (mappedResult !is ExecuteResult.Success) {
                    _screenState.postValue(mappedResult.asPgnState(screenState.value?.data.orEmpty()))
                } else {
                    // при успехе ожидаем feedPostsUpdateEvents
                }
            }
        }
    }
}