package net.maxsmr.vk_news_client.ui.presentation.news

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.maxsmr.commonutils.states.PgnLoadState
import net.maxsmr.vk_news_client.ui.model.FeedPostUI
import net.maxsmr.vk_news_client.ui.presentation.main.EmptyErrorScreen
import net.maxsmr.vk_news_client.ui.presentation.main.LoadingScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsFeedScreen(
    viewModel: NewsFeedViewModel,
    paddingValues: PaddingValues,
    onCommentClickListener: (FeedPostUI) -> Unit,
) {
    val modifier = Modifier.padding(paddingValues)
    val screenState = viewModel.screenState.observeAsState(PgnLoadState.pgnInitial()).value
    val refreshState = rememberPullToRefreshState()
    // для сохранения состояния скролла между рекомпозициями запоминаем стейт
    // до вызова функции с LazyColumn
    val listState = rememberLazyListState()
    if (screenState.isLoading) {
        if (screenState.hasData { !it.isNullOrEmpty() }) {
            when (screenState.loadingState) {
                is PgnLoadState.PgnLoading.MainLoad ->
                    // загрузка с первой страницы при наличии данных ->
                    // pull-to-refresh поверх
                    PullToRefreshBox(
                        modifier = Modifier.padding(top = 28.dp),
                        isRefreshing = true,
                        state = refreshState,
                        contentAlignment = Alignment.TopCenter,
                        onRefresh = {
                            viewModel.reloadRecommendations()
                        }
                    ) {
                        FeedPostCardList(
                            viewModel,
                            screenState.data.orEmpty(),
                            false,
                            modifier,
                            listState,
                            onCommentClickListener
                        )
                    }

                is PgnLoadState.PgnLoading.PageLoad -> {
                    // загрузка следующей страницы при наличии данных ->
                    // контейнер с данными и под ним крутилка
                    FeedPostCardList(
                        viewModel,
                        screenState.data.orEmpty(),
                        true,
                        modifier,
                        listState,
                        onCommentClickListener
                    )
                }

                else -> {
                    // не должно быть, т.к. screenState.isLoading = true
                }
            }
        } else {
            LoadingScreen(modifier)
        }
    } else {
        if (screenState.hasData { !it.isNullOrEmpty() }) {
            PullToRefreshBox(
                modifier = Modifier.padding(top = 28.dp),
                isRefreshing = false,
                state = refreshState,
                contentAlignment = Alignment.TopCenter,
                onRefresh = {
                    viewModel.reloadRecommendations()
                }) {
                FeedPostCardList(
                    viewModel,
                    screenState.data.orEmpty(),
                    false,
                    modifier,
                    listState,
                    onCommentClickListener
                )
            }
        } else {
            EmptyErrorScreen(screenState, modifier) {
                viewModel.reloadRecommendations()
            }
        }
    }
}