package net.maxsmr.mxstemplate.ui.fragment

import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.ui.components.IComponentDelegate
import net.maxsmr.core.ui.view.alert.delegate.ViewFragmentAlertDelegate
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.rate.alert.view.RateAppFragmentAlertDelegate
import net.maxsmr.feature.rate.fragment.BaseFeedbackFragment
import net.maxsmr.mxstemplate.mobileBuildType
import net.maxsmr.mxstemplate.ui.MainFeedbackViewModel
import net.maxsmr.mxstemplate.ui.delegate.MainRateAppComponentDelegate
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class MainFeedbackFragment: BaseFeedbackFragment<MainFeedbackViewModel>() {

    override val viewModel: MainFeedbackViewModel by viewModels()

    private val rateDelegate by lazy {
        MainRateAppComponentDelegate(
            requireActivity(),
            viewModel,
            null,
            mobileBuildType,
            cacheRepo
        )
    }

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    lateinit var cacheRepo: CacheDataStoreRepository

    override fun onSendClick() {
        super.onSendClick()
        if (viewModel.params.shouldNavigateToMarket) {
            rateDelegate.navigateToMarket()
        }
    }

    override fun createAlertDelegate(): ViewFragmentAlertDelegate<MainFeedbackViewModel> =
        RateAppFragmentAlertDelegate(rateDelegate, this, viewModel)

    override fun createFragmentDelegates(): List<IComponentDelegate<*>> {
        return listOf(rateDelegate)
    }
}