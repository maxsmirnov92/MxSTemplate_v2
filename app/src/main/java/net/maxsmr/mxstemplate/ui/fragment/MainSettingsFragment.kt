package net.maxsmr.mxstemplate.ui.fragment

import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.ui.components.IComponentDelegate
import net.maxsmr.core.ui.view.alert.delegate.CombinedViewFragmentAlertDelegate
import net.maxsmr.feature.about.alert.view.ReleaseNotesFragmentAlertDelegate
import net.maxsmr.feature.preferences.ui.SettingsFragment
import net.maxsmr.feature.preferences.ui.SettingsFragmentAlertDelegate
import net.maxsmr.feature.preferences.ui.SettingsViewModel
import net.maxsmr.feature.rate.RateAppReminderComponentDelegate
import net.maxsmr.mxstemplate.RATE_APP_ASK_INTERVAL
import net.maxsmr.mxstemplate.ui.fragment.params.AboutScreenParams

@AndroidEntryPoint
class MainSettingsFragment: SettingsFragment() {

    private val rateReminderDelegate by lazy {
        RateAppReminderComponentDelegate(
            requireContext(),
            viewModel,
            RATE_APP_ASK_INTERVAL,
            viewModel.cacheRepository,
        ) {
            viewModel.navigate(
                NavigationAction.NavigationCommand.ToDirectionWithNavDirections(
                    MainDownloadsPagerFragmentDirections.actionToAboutFragment(AboutScreenParams(true))
                )
            )
        }
    }

    override fun createAlertDelegate(): CombinedViewFragmentAlertDelegate<SettingsViewModel> {
        return CombinedViewFragmentAlertDelegate(
            listOf(
                SettingsFragmentAlertDelegate(this, viewModel),
                ReleaseNotesFragmentAlertDelegate(this, viewModel),
            ), this, viewModel
        )
    }

    override fun createFragmentDelegates(): List<IComponentDelegate<*>> {
        return listOf(rateReminderDelegate)
    }
}