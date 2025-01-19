package net.maxsmr.vk_news_client.ui.presentation.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.errorLoad
import net.maxsmr.commonutils.live.loading
import net.maxsmr.commonutils.live.successLoad
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.coroutines.execute.ExecuteResult
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

    private val _screenState = MutableLiveData<LoadState<List<FeedPostUI>>>(LoadState.initial())

    val screenState = _screenState as LiveData<LoadState<List<FeedPostUI>>>

    override fun onInitialized() {
        super.onInitialized()
        if (screenState.value?.wasLoaded != true) {
            loadRecommended()
        }
        viewModelScope.launch {
            repo.feedPostsUpdateEvents.collectLatest {
                _screenState.successLoad(it.map { post ->
                    post.toFeedPostUI()
                }, setValue = false)
            }
        }
    }

    fun loadRecommended() {
        _screenState.loading()
        viewModelScope.launch {
            when (val result = loadRecommendationsUseCase(Unit)) {
                is ExecuteResult.Error -> {
                    _screenState.errorLoad(result.exception, setValue = false)
                }

                else -> {

                }
            }
        }
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

        val currentList = currentState.data.orEmpty()
        val newList = currentList.map {
            if (it.id == id) {
                it.copy(statisticsItems = it.statisticsItems.map { statsItem ->
                    if (statsItem.type == type) {
                        statsItem.copy(count = statsItem.count + 1)
                    } else {
                        statsItem
                    }
                })
            } else {
                it
            }
        }
        _screenState.successLoad(newList)
    }

    fun delete(item: FeedPostUI) {
        val currentState = screenState.value ?: return
        if (!currentState.isSuccess) return

        val currentList = currentState.data.orEmpty()
        val newList = currentList.mapNotNull {
            if (it.id == item.id) {
                null
            } else {
                it
            }
        }
        _screenState.successLoad(newList)
    }
}