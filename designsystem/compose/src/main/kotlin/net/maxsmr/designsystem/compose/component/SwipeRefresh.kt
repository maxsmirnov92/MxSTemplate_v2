package net.maxsmr.designsystem.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import net.maxsmr.designsystem.compose.theme.AppColors


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwipeRefresh(
    refreshing: Boolean,
    empty: Boolean,
    emptyContent: @Composable () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pullRefreshState = rememberPullRefreshState(refreshing, onRefresh)

    if (empty) {
        emptyContent()
    } else {
        Box(
            modifier = modifier.pullRefresh(pullRefreshState)
        ) {
            content()

            PullRefreshIndicator(
                refreshing = refreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = AppColors.White,
                contentColor = AppColors.Red500
            )
        }
    }
}