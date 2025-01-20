package net.maxsmr.feature.compose_sample.ui.presentation.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import net.maxsmr.core.ui.compose.components.ComposableDependencies
import net.maxsmr.core.ui.compose.navigation.rememberNavigationState
import net.maxsmr.core.ui.compose.navigation.viewModel
import net.maxsmr.core.ui.location.LocationViewModel
import net.maxsmr.feature.compose_sample.ui.SampleComposeActivity
import net.maxsmr.feature.compose_sample.ui.navigation.AppNavGraph
import net.maxsmr.feature.compose_sample.ui.navigation.NavigationItem
import net.maxsmr.feature.compose_sample.ui.navigation.Screen
import net.maxsmr.feature.compose_sample.ui.presentation.favourite.FavouriteScreen
import net.maxsmr.feature.compose_sample.ui.presentation.favourite.FavouriteViewModel
import net.maxsmr.feature.compose_sample.ui.presentation.home.HomeScreen
import net.maxsmr.feature.compose_sample.ui.presentation.home.HomeViewModel
import net.maxsmr.feature.compose_sample.ui.presentation.profile.ProfileScreen
import net.maxsmr.feature.compose_sample.ui.presentation.profile.ProfileViewModel

@Composable
fun MainScreen(
    activity: SampleComposeActivity,
    locationViewModel: LocationViewModel,
    dependencies: ComposableDependencies
) {
    val navigationState = rememberNavigationState(dependencies.navHostController)

    val homeViewModel = viewModel<HomeViewModel>(Screen.ROUTE_HOME, activity, dependencies).viewModel
    val favouriteViewModel = viewModel<FavouriteViewModel>(Screen.ROUTE_FAVOURITE, activity, dependencies).viewModel
    val profileViewModel = viewModel<ProfileViewModel>(Screen.ROUTE_PROFILE, activity, dependencies).viewModel

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = dependencies.snackbarHostState)
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                val navBackStackEntry by navigationState.navHostController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val items = listOf(NavigationItem.Home, NavigationItem.Favorite, NavigationItem.Profile)
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.titleResId)) },
                        selected = currentRoute == item.screen.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSecondary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        onClick = {
                            navigationState.navigateTo(item.screen.route)
                        }
                    )
                }
            }
        }
    ) {
        Box(Modifier.padding(it)) {
            AppNavGraph(
                navHostController = navigationState.navHostController,
                homeScreenContent = {
                    HomeScreen(activity, locationViewModel, homeViewModel)
                },
                favoriteScreenContent = {
                    FavouriteScreen(favouriteViewModel)
                },
                profileScreenContent = {
                    ProfileScreen(profileViewModel)
                }
            )
        }
    }
}

@Composable
fun TextWithCounter(text: String) {
    var counter by rememberSaveable { // remember
        mutableIntStateOf(0)
    }
    Text(
        modifier = Modifier.clickable { counter++ },
        text = "$text, count: $counter",
        color = Color.Black
    )
}