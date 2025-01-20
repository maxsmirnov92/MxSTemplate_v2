package net.maxsmr.feature.compose_sample.ui

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.android.base.delegates.AbstractSavedStateViewModelFactory
import net.maxsmr.core.ui.compose.components.BaseComposeActivity
import net.maxsmr.core.ui.compose.components.ComposableDependencies
import net.maxsmr.core.ui.location.LocationViewModel
import net.maxsmr.feature.compose_sample.ui.presentation.main.MainScreen
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class SampleComposeActivity : BaseComposeActivity<SampleComposeViewModel>() {

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    override val viewModel: SampleComposeViewModel by viewModels()

    private val locationViewModel: LocationViewModel by viewModels {
        AbstractSavedStateViewModelFactory(this) {
            locationFactory.create(it, null)
        }
    }

    @Inject
    lateinit var locationFactory: LocationViewModel.Factory

    @Composable
    override fun SetScreenContent(dependencies: ComposableDependencies) {
        MainScreen(
            this,
            locationViewModel,
            dependencies
        )
    }

    override fun getExtraActivityScreenComponents(dependencies: ComposableDependencies): List<ScreenComponents> {
        return listOf(
            ScreenComponents(
                locationViewModel,
                null,
                getAlertDelegateForViewModel(locationViewModel,
                    lifecycleScope,
                    dependencies.snackbarHostState)
            )
        )
    }
}