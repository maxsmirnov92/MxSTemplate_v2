package net.maxsmr.mxstemplate.ui.fragment

import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.di.DI_NAME_RATE_APP_ASK_INTERVAL
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.core.ui.components.activities.BaseActivity
import net.maxsmr.core.ui.components.activities.BaseActivity.Companion.REQUEST_CODE_IN_APP_UPDATES
import net.maxsmr.feature.download.ui.BaseDownloadsPagerFragment
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.rate.RateAppReminderFragmentDelegate
import net.maxsmr.mobile_services.IMobileServicesAvailability
import net.maxsmr.mobile_services.update.ui.InAppUpdatesFragmentDelegate
import net.maxsmr.mxstemplate.mobileBuildType
import net.maxsmr.mxstemplate.mobileServicesAvailability
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class MainDownloadsPagerFragment: BaseDownloadsPagerFragment() {

    override val delegates: List<IFragmentDelegate> by lazy { listOf(rateDelegate, appUpdateDelegate) }

    private val appUpdateDelegate =  InAppUpdatesFragmentDelegate(
        this,
        REQUEST_CODE_IN_APP_UPDATES,
        mobileServicesAvailability,
        mobileBuildType)

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

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    lateinit var cacheRepo: CacheDataStoreRepository

    @Inject
    lateinit var availability: IMobileServicesAvailability

    @Inject
    @Named(DI_NAME_RATE_APP_ASK_INTERVAL)
    @JvmField
    var rateAskInterval: Long = 0

}