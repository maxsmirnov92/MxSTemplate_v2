package net.maxsmr.vk_news_client.ui.presentation.comments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.vk_news_client.ui.model.FeedPostCommentUI
import net.maxsmr.vk_news_client.ui.model.FeedPostUI

class CommentsViewModel @AssistedInject constructor(
    @Assisted private val post: FeedPostUI,
    @Assisted state: SavedStateHandle,
) : BaseViewModel(state) {

    private val _screenState = MutableLiveData<CommentsScreenState>(CommentsScreenState.Initial)
    val screenState: LiveData<CommentsScreenState> = _screenState

    override fun onInitialized() {
        super.onInitialized()
        loadComments(post)
    }

    fun loadComments(post: FeedPostUI) {
        val comments = mutableListOf<FeedPostCommentUI>().apply {
            repeat(10) {
                add(FeedPostCommentUI(it, "Author", "", "Long comment text", "14:00"))
            }
        }
        _screenState.value = CommentsScreenState.Comments(
            post = post,
            comments = comments
        )
    }

    @AssistedFactory
    interface Factory {

        fun create(
            post: FeedPostUI,
            state: SavedStateHandle,
        ): CommentsViewModel
    }
}