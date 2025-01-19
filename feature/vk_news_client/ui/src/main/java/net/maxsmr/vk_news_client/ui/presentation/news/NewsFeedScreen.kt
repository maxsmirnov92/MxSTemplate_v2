package net.maxsmr.vk_news_client.ui.presentation.news

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.maxsmr.commonutils.gui.message.errorMessage
import net.maxsmr.commonutils.gui.message.formatMessage
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.designsystem.compose.component.EmptyErrorContainer
import net.maxsmr.vk_news_client.ui.model.FeedPostUI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsFeedScreen(
    viewModel: NewsFeedViewModel,
    paddingValues: PaddingValues,
    onCommentClickListener: (FeedPostUI) -> Unit,
) {
    val modifier = Modifier.padding(paddingValues)
    val screenState = viewModel.screenState.observeAsState(LoadState.initial()).value
    val refreshState = rememberPullToRefreshState()
    if (screenState.isLoading) {
        if (screenState.hasData { !it.isNullOrEmpty() }) {
            PullToRefreshBox(
                modifier = Modifier.padding(top = 28.dp),
                isRefreshing = true,
                state = refreshState,
                contentAlignment = Alignment.TopCenter,
                onRefresh = {
                    viewModel.loadRecommended()
                }
            ) {
                FeedPostCardList(
                    viewModel,
                    screenState.data.orEmpty(),
                    modifier,
                    onCommentClickListener
                )
            }
        } else {
            FeedPostCardLoading(modifier)
        }
    } else {
        if (screenState.hasData { !it.isNullOrEmpty() }) {
            PullToRefreshBox(
                modifier = Modifier.padding(top = 28.dp),
                isRefreshing = false,
                state = refreshState,
                contentAlignment = Alignment.TopCenter,
                onRefresh = {
                    viewModel.loadRecommended()
                }) {
                FeedPostCardList(
                    viewModel,
                    screenState.data.orEmpty(),
                    modifier,
                    onCommentClickListener
                )
            }
        } else {
            EmptyErrorContainer(
                if (screenState.isError) {
                    screenState.error?.errorMessage().formatMessage(
                        net.maxsmr.core.android.R.string.error_format,
                        net.maxsmr.core.network.R.string.error_unexpected_try_again
                    ).get(LocalContext.current).toString()
                } else {
                    LocalContext.current.getString(net.maxsmr.core.android.R.string.no_data)
                },
                buttonResId = if (screenState.wasLoaded) {
                    net.maxsmr.core.android.R.string.try_again
                } else {
                    null
                },
                modifier = modifier
            ) {
                viewModel.loadRecommended()
            }
        }
    }
}