package net.maxsmr.mxstemplate.ui.delegate

import android.app.Activity
import dagger.hilt.android.EntryPointAccessors
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.ui.R
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.rate.BaseRateAppComponentDelegate
import net.maxsmr.mobile_services.IMobileServicesAvailability
import net.maxsmr.mobile_services.MobileBuildType
import net.maxsmr.mxstemplate.BuildConfig
import net.maxsmr.mxstemplate.di.MobileServicesEntryPoint
import net.maxsmr.mxstemplate.ui.fragment.MainAboutFragmentDirections
import net.maxsmr.mxstemplate.ui.fragment.params.FeedbackScreenParams

class MainRateAppComponentDelegate(
    host: Activity,
    viewModel: BaseViewModel,
    availability: IMobileServicesAvailability?,
    mobileBuildType: MobileBuildType,
    repo: CacheDataStoreRepository,
): BaseRateAppComponentDelegate(host, viewModel, availability, mobileBuildType, repo) {

    override fun navigateToMarket() {
        if (!EntryPointAccessors.fromApplication(
                    host,
                    MobileServicesEntryPoint::class.java
                ).marketIntentLauncher.startActivityMarketIntent(host)
        ) {
            viewModel.showToast(TextMessage(R.string.error_intent_open_market))
        }
    }

    override fun navigateToFeedback(shouldNavigateToMarket: Boolean) {
        viewModel.navigate(
            NavigationAction.NavigationCommand.ToDirectionWithNavDirections(
                MainAboutFragmentDirections.actionToFeedbackFragment(
                    FeedbackScreenParams(BuildConfig.DEV_EMAIL_ADDRESS, shouldNavigateToMarket)
                )
            )
        )
    }
}