package net.maxsmr.mxstemplate.ui

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.lifecycle.HiltViewModel
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.ui.R
import net.maxsmr.feature.about.BaseAboutViewModel
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.mxstemplate.BuildConfig
import net.maxsmr.mxstemplate.di.EntryPointFeatureMobileServices
import net.maxsmr.mxstemplate.ui.fragment.MainAboutFragmentArgs
import net.maxsmr.mxstemplate.ui.fragment.MainAboutFragmentDirections
import javax.inject.Inject

@HiltViewModel
class MainAboutViewModel @Inject constructor(
    override val repo: CacheDataStoreRepository,
    state: SavedStateHandle,
) : BaseAboutViewModel(state) {

    private val isForRate: Boolean = if (state.contains("isForRate")) {
        MainAboutFragmentArgs.fromSavedStateHandle(state).isForRate
    } else {
        false
    }

    override fun onInitialized() {
        super.onInitialized()
        if (isForRate) {
            onRateAppSelected()
        }
    }

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
                MainAboutFragmentDirections.actionToFeedbackFragment(BuildConfig.DEV_EMAIL_ADDRESS)
            )
        )
    }
}