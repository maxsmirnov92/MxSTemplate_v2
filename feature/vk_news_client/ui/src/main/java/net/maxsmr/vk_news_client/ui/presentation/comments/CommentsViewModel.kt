package net.maxsmr.vk_news_client.ui.presentation.comments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.live.errorLoad
import net.maxsmr.commonutils.live.loading
import net.maxsmr.commonutils.live.successLoad
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.coroutines.execute.ExecuteResult
import net.maxsmr.vk_news_client.data.usecase.LoadCommentsUseCase
import net.maxsmr.vk_news_client.ui.model.FeedPostUI
import net.maxsmr.vk_news_client.ui.model.toFeedPost
import net.maxsmr.vk_news_client.ui.model.toFeedPostCommentUI

class CommentsViewModel @AssistedInject constructor(
    @Assisted private val post: FeedPostUI,
    @Assisted state: SavedStateHandle,
    private val loadCommentsUseCase: LoadCommentsUseCase,
) : BaseViewModel(state) {

    private val _screenState = MutableLiveData<LoadState<CommentsScreenData>>(LoadState.initial())
    val screenState: LiveData<LoadState<CommentsScreenData>> = _screenState

    override fun onInitialized() {
        super.onInitialized()
        loadComments(post)
    }

    fun loadComments(post: FeedPostUI) {
        viewModelScope.launch {
            loadCommentsUseCase(post.toFeedPost()).collectLatest {
                when(it) {
                    is ExecuteResult.Loading -> {
                        _screenState.loading()
                    }
                    is ExecuteResult.Error -> {
                        _screenState.errorLoad(it.errorData())
                    }
                    is ExecuteResult.Success -> {
                        _screenState.successLoad(
                            CommentsScreenData(
                                post,
                                it.data.map { comment -> comment.toFeedPostCommentUI() }
                            ))
                    }
                    else -> {

                    }
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {

        fun create(
            post: FeedPostUI,
            state: SavedStateHandle,
        ): CommentsViewModel
    }
}