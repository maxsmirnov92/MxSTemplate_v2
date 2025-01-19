package net.maxsmr.vk_news_client.ui.presentation.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.ui.compose.components.ComposableDependencies
import net.maxsmr.core.ui.compose.components.IComposableViewModelsContainer
import net.maxsmr.core.ui.compose.navigation.hiltViewModel
import net.maxsmr.core.ui.compose.navigation.rememberNavigationState
import net.maxsmr.core.ui.compose.navigation.viewModel
import net.maxsmr.feature.vk_news_client.ui.R
import net.maxsmr.vk_news_client.ui.navigation.AppNavGraph
import net.maxsmr.vk_news_client.ui.navigation.BottomNavigationItem
import net.maxsmr.vk_news_client.ui.navigation.Screen
import net.maxsmr.vk_news_client.ui.presentation.comments.CommentsFactoryArgs
import net.maxsmr.vk_news_client.ui.presentation.comments.CommentsScreen
import net.maxsmr.vk_news_client.ui.presentation.comments.CommentsViewModel
import net.maxsmr.vk_news_client.ui.presentation.news.NewsFeedScreen
import net.maxsmr.vk_news_client.ui.presentation.news.NewsFeedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModelContainer: IComposableViewModelsContainer,
    dependencies: ComposableDependencies,
    onLogoutAction: () -> Unit,
) {
    val navigationState = rememberNavigationState(dependencies.navHostController)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = dependencies.snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.vk_news_client_feature_name))
                },
                actions = {
                    Text(
                        modifier = Modifier
                            .padding(14.dp)
                            .clickable(
//                            interactionSource = remember { MutableInteractionSource() },
//                            indication = LocalIndication.current,
                            ) {
                                onLogoutAction()
                            },
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        text = "logout"
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                val navBackStackEntry by navigationState.navHostController.currentBackStackEntryAsState()
//                val currentRoute = navBackStackEntry?.destination?.route
                val items =
                    listOf(
                        BottomNavigationItem.Home,
                        BottomNavigationItem.Favorite,
                        BottomNavigationItem.Profile
                    )
                items.forEach { item ->
                    val selected = navBackStackEntry?.destination?.hierarchy?.any {
                        it.route == item.screen.route
                    } ?: false
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.titleResId)) },
                        selected = selected, // currentRoute == item.screen.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSecondary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        onClick = {
                            if (!selected) {
                                navigationState.navigateTo(item.screen.route)
                            }
                        }
                    )
                }
            }
        }
    ) {
        AppNavGraph(
            navHostController = navigationState.navHostController,
            newsScreenContent = {
                // при переходах назад по бэкстеку те же composable функции вызываются заново;
                // экран пересоздаётся, стейт восстанавливается вручную
                // (здесь rememberLazyListState)
                // при navigate в новый функция вызывается заново, стейт с начальным значением

                val newsVmResult =
                    hiltViewModel<NewsFeedViewModel>(Screen.ROUTE_NEWS_FEED, viewModelContainer, dependencies)

                NewsFeedScreen(newsVmResult.viewModel, paddingValues = it) { post ->
                    // или navigationState.navHostController.navigate(Screen.Comments.getRouteWithArgs(post))
                    newsVmResult.viewModel.navigate(
                        NavigationAction.NavigationCommand.ToDirectionWithRoute(
                            Screen.Comments.getRouteWithArgs(
                                post
                            )
                        )
                    )
                }
            },
            commentsScreenContent = { post ->
                val commentsViewModel = viewModel<CommentsViewModel>(
                    Screen.ROUTE_COMMENTS,
                    viewModelContainer,
                    dependencies,
                    factory = viewModelContainer.getFactoryForViewModel(
                        CommentsViewModel::class.java,
                        CommentsFactoryArgs(post)
                    )
                )
                CommentsScreen(commentsViewModel.viewModel, Modifier.padding(it), post = post) { // commentsToPost.value!!
                    navigationState.navHostController.popBackStack()
                }
            },
            favoriteScreenContent = {
                TextWithCounter("Favorite", Modifier.padding(it))
            },
            profileScreenContent = {
                TextWithCounter("Profile", Modifier.padding(it))
            }
        )
    }
}


@Composable
private fun TextWithCounter(text: String, modifier: Modifier = Modifier) {
    var counter by rememberSaveable { // remember
        mutableIntStateOf(0)
    }
    Text(
        modifier = modifier.clickable { counter++ },
        text = "$text, count: $counter",
        color = Color.Black
    )
}