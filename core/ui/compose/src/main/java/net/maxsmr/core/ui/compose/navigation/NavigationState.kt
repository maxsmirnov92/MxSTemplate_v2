package net.maxsmr.core.ui.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

class NavigationState(
    val navHostController: NavHostController,
) {

    /**
     * Для навигации в пределах BottomBar
     */
    fun navigateTo(route: String) {
//        if (route != navHostController.currentDestination?.route) {
        navHostController.navigate(route) {
            // при переходах удаление экранов из бэкстэка до Home или того, что на нём
            popUpTo(navHostController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
//        }
    }
}

@Composable
fun rememberNavigationState(
    navHostController: NavHostController = rememberNavController(),
): NavigationState {
    return remember {
        NavigationState(navHostController)
    }
}
