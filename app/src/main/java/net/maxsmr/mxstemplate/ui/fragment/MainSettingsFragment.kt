package net.maxsmr.mxstemplate.ui.fragment

import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.feature.preferences.ui.SettingsFragment
import net.maxsmr.feature.rate.RateAppReminderFragmentDelegate
import net.maxsmr.mxstemplate.RATE_APP_ASK_INTERVAL

@AndroidEntryPoint
class MainSettingsFragment: SettingsFragment() {

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