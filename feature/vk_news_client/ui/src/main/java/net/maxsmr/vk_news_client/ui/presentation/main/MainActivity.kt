package net.maxsmr.vk_news_client.ui.presentation.main

import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.delegates.AbstractSavedStateViewModelFactory
import net.maxsmr.core.ui.alert.ConnectionHandler
import net.maxsmr.core.ui.compose.components.BaseComposeActivity
import net.maxsmr.core.ui.compose.components.ComposableDependencies
import net.maxsmr.core.ui.compose.components.IComposableViewModelsContainer
import net.maxsmr.permissionchecker.PermissionsHelper
import net.maxsmr.vk_news_client.ui.presentation.comments.CommentsFactoryArgs
import net.maxsmr.vk_news_client.ui.presentation.comments.CommentsViewModel
import net.maxsmr.vk_news_client.ui.presentation.login.LoginErrorScreen
import net.maxsmr.vk_news_client.ui.presentation.login.LoginScreenOneTap
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseComposeActivity<MainViewModel>() {

    override val viewModel: MainViewModel by viewModels()

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    lateinit var commentsVmFactory: CommentsViewModel.Factory

    @Composable
    override fun SetScreenContent(dependencies: ComposableDependencies) {
        when (val state = viewModel.authState.observeAsState(AuthState.NotAuthorized).value) {
            is AuthState.NotAuthorized -> {
                LoginScreenOneTap(this,
                    dependencies.snackbarHostState,
                    {
                        viewModel.onAuthSuccess(it)
                    },
                    {
                        viewModel.onAuthFailed(it)
                    })
            }

            is AuthState.Authorized -> {
                MainScreen(this@MainActivity, dependencies) {
                    viewModel.logout()
                }
            }

            is AuthState.AuthFailed -> {
                LoginErrorScreen(dependencies.snackbarHostState, state.description) {
                    viewModel.logout()
                }
            }
        }
    }

    override fun <VM : BaseViewModel> getFactoryForViewModel(
        viewModelClass: Class<VM>,
        args: IComposableViewModelsContainer.IFactoryArgs<VM>?,
    ): ViewModelProvider.Factory? {
        return if (viewModelClass.isAssignableFrom(CommentsViewModel::class.java)) {
            AbstractSavedStateViewModelFactory(this) {
                commentsVmFactory.create((args as CommentsFactoryArgs).feedPost, it)
            }
        } else {
            null
        }
    }

    override fun getConnectionHandlerForActivityViewModel(
        scope: CoroutineScope,
        hostState: SnackbarHostState,
    ) = ConnectionHandler()
}

