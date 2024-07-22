package net.maxsmr.mxstemplate.ui.fragment

import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.rate.fragment.BaseFeedbackFragment
import net.maxsmr.mobile_services.IMobileServicesAvailability
import net.maxsmr.mxstemplate.mobileBuildType
import net.maxsmr.mxstemplate.ui.MainFeedbackViewModel
import net.maxsmr.mxstemplate.ui.delegate.MainRateAppFragmentDelegate
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class MainFeedbackFragment: BaseFeedbackFragment<MainFeedbackViewModel>() {

    private val args by navArgs<MainFeedbackFragmentArgs>()

    override val viewModel: MainFeedbackViewModel by viewModels()

    private val rateDelegate by lazy {
        MainRateAppFragmentDelegate(
            this,
            viewModel,
            null,
            mobileBuildType,
            cacheRepo
        )
    }

    override val delegates: List<IFragmentDelegate> by lazy { listOf(rateDelegate) }

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    @Inject
    lateinit var cacheRepo: CacheDataStoreRepository

    override fun onSendClick() {
        super.onSendClick()
        if (args.shouldNavigateToMarket) {
            rateDelegate.navigateToMarket()
        }
    }
}