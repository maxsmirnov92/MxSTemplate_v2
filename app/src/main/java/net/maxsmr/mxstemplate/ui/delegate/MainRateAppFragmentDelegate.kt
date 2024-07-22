package net.maxsmr.mxstemplate.ui.delegate

import dagger.hilt.android.EntryPointAccessors
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.ui.R
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.rate.BaseRateAppFragmentDelegate
import net.maxsmr.mobile_services.IMobileServicesAvailability
import net.maxsmr.mobile_services.MobileBuildType
import net.maxsmr.mxstemplate.BuildConfig
import net.maxsmr.mxstemplate.di.MobileServicesFeatureEntryPoint
import net.maxsmr.mxstemplate.ui.fragment.MainAboutFragmentDirections

class MainRateAppFragmentDelegate(
    fragment: BaseVmFragment<*>,
    viewModel: BaseViewModel,
    availability: IMobileServicesAvailability?,
    mobileBuildType: MobileBuildType,
    repo: CacheDataStoreRepository,
): BaseRateAppFragmentDelegate(fragment, viewModel, availability, mobileBuildType, repo) {

    override fun navigateToMarket() {
        if (!EntryPointAccessors.fromApplication(
                    baseApplicationContext,
                    MobileServicesFeatureEntryPoint::class.java
                ).marketIntentLauncher.startActivityMarketIntent(fragment.requireActivity())
        ) {
            viewModel.showToast(TextMessage(R.string.toast_market_not_installed))
        }
    }

    override fun navigateToFeedback(shouldNavigateToMarket: Boolean) {
        viewModel.navigate(
            NavigationAction.NavigationCommand.ToDirectionWithNavDirections(
                MainAboutFragmentDirections.actionToFeedbackFragment(BuildConfig.DEV_EMAIL_ADDRESS, shouldNavigateToMarket)
            )
        )
    }
}