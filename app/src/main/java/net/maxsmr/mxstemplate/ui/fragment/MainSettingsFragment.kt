package net.maxsmr.mxstemplate.ui.fragment

import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.di.DI_NAME_RATE_APP_ASK_INTERVAL
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.ui.BaseSettingsFragment
import net.maxsmr.feature.rate.RateAppReminderFragmentDelegate
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainSettingsFragment: BaseSettingsFragment() {

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    @Named(DI_NAME_RATE_APP_ASK_INTERVAL)
    @JvmField
    var rateAskInterval: Long = 0

    private val rateDelegate by lazy {
        RateAppReminderFragmentDelegate(
            rateAskInterval,
            viewModel.cacheRepository
        ) {
            viewModel.navigate(
                NavigationAction.NavigationCommand.ToDirectionWithNavDirections(
                    MainDownloadsPagerFragmentDirections.actionToAboutFragment(true)
                )
            )
        }
    }

    override val delegates: List<IFragmentDelegate> by lazy { listOf(rateDelegate) }
}