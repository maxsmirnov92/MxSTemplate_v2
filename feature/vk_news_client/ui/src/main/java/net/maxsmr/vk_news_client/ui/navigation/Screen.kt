package net.maxsmr.vk_news_client.ui.navigation

import net.maxsmr.core.domain.entities.feature.vk_news_client.FeedPost
import net.maxsmr.core.ui.compose.navigation.NavTypeHolder
import net.maxsmr.core.ui.compose.navigation.getRouteWithArgs
import net.maxsmr.vk_news_client.ui.model.FeedPostUI

sealed class Screen (
    val route: String
) {
    data object Home: Screen(ROUTE_HOME)
    data object Favourite: Screen(ROUTE_FAVOURITE)
    data object Profile: Screen(ROUTE_PROFILE)
    data object NewsFeed: Screen(ROUTE_NEWS_FEED)

    data object Comments: Screen(ROUTE_COMMENTS) {

        private const val ROUTE_FOR_ARGS = "comments"

        fun getRouteWithArgs(post: FeedPostUI) = getRouteWithArgs(ROUTE_FOR_ARGS, NavTypeHolder.encodeArg(post))
    }

    companion object {

        const val ARG_FEED_POST = "feed_post"

        const val ROUTE_HOME = "home"
        const val ROUTE_FAVOURITE = "favourite"
        const val ROUTE_PROFILE = "profile"
        const val ROUTE_NEWS_FEED = "news_feed"
        const val ROUTE_COMMENTS = "comments/{$ARG_FEED_POST}"
    }
}