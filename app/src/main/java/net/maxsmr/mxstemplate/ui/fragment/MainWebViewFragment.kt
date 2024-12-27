package net.maxsmr.mxstemplate.ui.fragment

import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.ui.components.IComponentDelegate
import net.maxsmr.feature.download.ui.webview.BaseDownloadableWebViewFragment
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.rate.RateAppReminderComponentDelegate
import net.maxsmr.feature.rate.alert.view.RateAppReminderFragmentAlertDelegate
import net.maxsmr.mxstemplate.RATE_APP_ASK_INTERVAL
import net.maxsmr.mxstemplate.ui.MainWebViewModel
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class MainWebViewFragment: BaseDownloadableWebViewFragment<MainWebViewModel>() {

//    private val args by navArgs<MainWebViewFragmentArgs>()

    override val viewModel: MainWebViewModel by viewModels()

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    lateinit var cacheRepo: CacheDataStoreRepository

    private val rateReminderDelegate by lazy {
        RateAppReminderComponentDelegate(
            requireContext(),
            viewModel,
            RATE_APP_ASK_INTERVAL,
            cacheRepo
        ) {
            viewModel.navigate(
                NavigationAction.NavigationCommand.ToDirectionWithNavDirections(
                    MainDownloadsPagerFragmentDirections.actionToAboutFragment(true)
                )
            )
        }
    }

    override fun createAlertDelegate(): RateAppReminderFragmentAlertDelegate<MainWebViewModel> {
        return RateAppReminderFragmentAlertDelegate(rateReminderDelegate, this, viewModel)
    }

    override fun createFragmentDelegates(): List<IComponentDelegate<*>> {
        return listOf(rateReminderDelegate)
    }
}