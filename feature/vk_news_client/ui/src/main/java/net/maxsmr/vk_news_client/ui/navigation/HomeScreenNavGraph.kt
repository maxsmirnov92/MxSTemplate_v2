package net.maxsmr.vk_news_client.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import net.maxsmr.commonutils.getParcelableCompat
import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPost
import net.maxsmr.core.ui.compose.navigation.NavTypeHolder
import net.maxsmr.vk_news_client.ui.model.FeedPostUI
import net.maxsmr.vk_news_client.ui.navigation.Screen.Companion.ARG_FEED_POST

fun NavGraphBuilder.homeScreenNavGraph(
    newsScreenContent: @Composable () -> Unit,
    commentsScreenContent: @Composable (FeedPostUI) -> Unit,
) {
    navigation(
        startDestination = Screen.NewsFeed.route,
        route = Screen.Home.route
    ) {
        composable(Screen.NewsFeed.route) {
            newsScreenContent()
        }
        composable(
            route = Screen.Comments.route,
            arguments = listOf(
                navArgument(ARG_FEED_POST) {
                    // по умолчанию NavType.StringType
                    type = NavTypeHolder.getParcelableNavType<FeedPostUI>()
                }
            )
        ) {
            val post: FeedPostUI = it.arguments?.getParcelableCompat(ARG_FEED_POST) ?: throw RuntimeException()
            commentsScreenContent(post)
        }
    }
}

