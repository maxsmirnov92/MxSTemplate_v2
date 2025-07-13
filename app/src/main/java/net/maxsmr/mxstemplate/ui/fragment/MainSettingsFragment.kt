package net.maxsmr.mxstemplate.ui.fragment

import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.ui.components.IComponentDelegate
import net.maxsmr.core.ui.view.alert.delegate.FragmentViewAlertDelegate
import net.maxsmr.feature.preferences.ui.SettingsFragment
import net.maxsmr.feature.preferences.ui.SettingsFragmentAlertDelegate
import net.maxsmr.feature.rate.RateAppReminderComponentDelegate
import net.maxsmr.feature.rate.alert.view.RateAppReminderFragmentAlertDelegate
import net.maxsmr.mxstemplate.RATE_APP_ASK_INTERVAL
import net.maxsmr.mxstemplate.ui.fragment.params.AboutScreenParams

@AndroidEntryPoint
class MainSettingsFragment : SettingsFragment() {

    private val rateReminderDelegate by lazy {
        RateAppReminderComponentDelegate(
            requireContext(),
            viewModel,
            RATE_APP_ASK_INTERVAL,
            viewModel.cacheRepository,
        ) {
            viewModel.navigate(
                NavigationAction.NavigationCommand.ToDirectionWithNavDirections(
                    MainSettingsFragmentDirections.actionToAboutFragment(AboutScreenParams(true))
                )
            )
        }
    }

    override fun createAlertDelegate() = FragmentViewAlertDelegate(
        this,
        viewModel,
        SettingsFragmentAlertDelegate(this, viewModel),
        RateAppReminderFragmentAlertDelegate(this, viewModel, rateReminderDelegate)
    )

    override fun createFragmentDelegates(): List<IComponentDelegate<*>> {
        return listOf(rateReminderDelegate)
    }
}