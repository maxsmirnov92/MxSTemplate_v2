package net.maxsmr.vk_news_client.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import net.maxsmr.feature.vk_news_client.ui.R

sealed class BottomNavigationItem(
    val screen: Screen,
    @StringRes
    val titleResId: Int,
    val icon: ImageVector
) {

    data object Home: BottomNavigationItem(
        screen = Screen.Home,
        titleResId = R.string.vk_news_client_navigation_item_main,
        icon = Icons.Outlined.Home
    )

    data object Favorite: BottomNavigationItem(
        screen = Screen.Favourite,
        titleResId = R.string.vk_news_client_navigation_item_favorite,
        icon = Icons.Outlined.Favorite
    )

    data object Profile: BottomNavigationItem(
        screen = Screen.Profile,
        titleResId = R.string.vk_news_client_navigation_item_profile,
        icon = Icons.Outlined.Person
    )
}