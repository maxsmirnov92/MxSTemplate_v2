package net.maxsmr.feature.compose_sample.ui.navigation

sealed class Screen (
    val route: String
) {

    data object Home: Screen(ROUTE_HOME)
    data object Favourite: Screen(ROUTE_FAVOURITE)
    data object Profile: Screen(ROUTE_PROFILE)

    companion object {

        const val ROUTE_HOME = "home"
        const val ROUTE_FAVOURITE = "favourite"
        const val ROUTE_PROFILE = "profile"
    }
}