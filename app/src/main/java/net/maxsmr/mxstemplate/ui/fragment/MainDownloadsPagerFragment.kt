package net.maxsmr.mxstemplate.ui.fragment

import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.di.DI_NAME_RATE_APP_ASK_INTERVAL
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.feature.download.ui.BaseDownloadsPagerFragment
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.rate.RateAppReminderFragmentDelegate
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainDownloadsPagerFragment: BaseDownloadsPagerFragment() {

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    lateinit var cacheRepo: CacheDataStoreRepository

    @Inject
    @Named(DI_NAME_RATE_APP_ASK_INTERVAL)
    @JvmField
    var rateAskInterval: Long = 0

    private val rateDelegate by lazy {
        RateAppReminderFragmentDelegate(
            rateAskInterval,
            cacheRepo
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