package net.maxsmr.justupdownloadit.ui.fragment

import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.feature.preferences.ui.BaseSettingsFragment
import net.maxsmr.feature.rate.RateAppReminderFragmentDelegate
import net.maxsmr.justupdownloadit.RATE_APP_ASK_INTERVAL
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class MainSettingsFragment: BaseSettingsFragment() {

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    private val rateDelegate by lazy {
        RateAppReminderFragmentDelegate(
            this,
            viewModel,
            RATE_APP_ASK_INTERVAL,
            viewModel.cacheRepository,
        ) {
            viewModel.navigate(
                NavigationAction.NavigationCommand.ToDirectionWithNavDirections(
                    MainDownloadsPagerFragmentDirections.actionToAboutFragment(true)
                )
            )
        }
    }

    override fun createFragmentDelegates(): List<IFragmentDelegate> {
        return listOf(rateDelegate)
    }
}