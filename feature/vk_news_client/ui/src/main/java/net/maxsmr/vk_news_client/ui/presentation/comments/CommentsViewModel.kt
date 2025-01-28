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
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.gui.message.errorMessage
import net.maxsmr.commonutils.gui.message.formatMessage
import net.maxsmr.commonutils.live.errorLoad
import net.maxsmr.commonutils.live.loading
import net.maxsmr.commonutils.live.successLoad
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.coroutines.execute.ExecuteResult
import net.maxsmr.core.android.coroutines.execute.asState
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPostComment
import net.maxsmr.core.network.api.VkNewsDataSource
import net.maxsmr.core.ui.field.createTextField
import net.maxsmr.feature.vk_news_client.ui.R
import net.maxsmr.vk_news_client.data.repository.CommentsRepositoryImpl
import net.maxsmr.vk_news_client.data.usecase.CreateCommentUseCase
import net.maxsmr.vk_news_client.data.usecase.LoadCommentsUseCase
import net.maxsmr.vk_news_client.ui.model.FeedPostUI
import net.maxsmr.vk_news_client.ui.model.toFeedPost
import net.maxsmr.vk_news_client.ui.model.toFeedPostCommentUI

class CommentsViewModel @AssistedInject constructor(
    @Assisted state: SavedStateHandle,
    @Assisted private val post: FeedPostUI,
    vkNewsDataSource: VkNewsDataSource,
) : BaseViewModel(state) {

    private val repo = CommentsRepositoryImpl(vkNewsDataSource, post.toFeedPost())

    private val loadCommentsUseCase: LoadCommentsUseCase = LoadCommentsUseCase(repo)

    private val createCommentUseCase: CreateCommentUseCase = CreateCommentUseCase(repo)

    private val _screenState = MutableLiveData<LoadState<CommentsScreenData>>(LoadState.initial())
    val screenState: LiveData<LoadState<CommentsScreenData>> = _screenState

    private val _commentsLoadState = MutableLiveData<LoadState<FeedPostComment>>()
    val commentsLoadState: LiveData<LoadState<FeedPostComment>> = _commentsLoadState

    val commentField = createTextField {
        hint(R.string.vk_news_client_comment_hint)
        setRequired(true, net.maxsmr.core.ui.R.string.field_error_empty)
    }

    override fun onInitialized() {
        super.onInitialized()

        viewModelScope.launch {
            repo.lastComments.collectLatest {
                _screenState.successLoad(
                    CommentsScreenData(
                        post,
                        repo.comments.value.map {
                                comment -> comment.toFeedPostCommentUI()
                        }
                    ),
                    setValue = false
                )
            }
        }

        commentField.valueFlow.observe {
            commentField.clearError()
        }
        commentsLoadState.bindProgress(
            message = TextMessage(net.maxsmr.core.android.R.string.loading)
        ).observeForever {
            when {
                it.isError -> {
                    showSnackbar(
                        it.error?.errorMessage().formatMessage(
                            net.maxsmr.core.network.R.string.error_request_failed_format,
                            net.maxsmr.core.network.R.string.error_request_failed,
                        )
                    )
                }

                it.hasData() -> {
                    commentField.value = EMPTY_STRING
                }
            }
        }

        loadComments()
    }

    fun loadComments() {
        viewModelScope.launch {
            loadCommentsUseCase(Unit).collectLatest {
                when (it) {
                    is ExecuteResult.Loading -> {
                        _screenState.loading(setValue = false)
                    }

                    is ExecuteResult.Error -> {
                        _screenState.errorLoad(it.errorData(), setValue = false)
                    }

                    else -> {

                    }
                }
            }
        }
    }

    fun createComment() {
        if (!commentField.validateAndSet()) {
            return
        }
        viewModelScope.launch {
            createCommentUseCase.invoke(commentField.value).collectLatest {
                _commentsLoadState.postValue(it.asState())
            }
        }
    }

    @AssistedFactory
    interface Factory {

        fun create(
            state: SavedStateHandle,
            post: FeedPostUI,
        ): CommentsViewModel
    }
}