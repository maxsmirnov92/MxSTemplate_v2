package net.maxsmr.mxstemplate.ui

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.lifecycle.HiltViewModel
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.android.base.actions.ToastAction
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.ui.R
import net.maxsmr.feature.about.BaseAboutViewModel
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.mxstemplate.BuildConfig
import net.maxsmr.mxstemplate.di.EntryPointFeatureMobileServices
import net.maxsmr.mxstemplate.ui.fragment.AboutFragmentDirections
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    override val repo: CacheDataStoreRepository,
    state: SavedStateHandle,
) : BaseAboutViewModel(state) {

    override fun navigateToMarket(activity: Activity) {
        if (!EntryPointAccessors.fromApplication(
                    baseApplicationContext,
                    EntryPointFeatureMobileServices::class.java
                ).marketIntentLauncher.startActivityMarketIntent(activity)
        ) {
            showToast(TextMessage(R.string.toast_market_not_installed))
        }
    }

    override fun navigateToFeedback() {
        navigate(
            NavigationAction.NavigationCommand.ToDirectionWithNavDirections(
                AboutFragmentDirections.actionToFeedbackFragment(BuildConfig.DEV_EMAIL_ADDRESS)
            )
        )
    }
}