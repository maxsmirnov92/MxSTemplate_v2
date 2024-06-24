package net.maxsmr.mxstemplate.ui

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.lifecycle.HiltViewModel
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.feature.about.BaseAboutViewModel
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.mxstemplate.di.EntryPointFeatureMobileServices
import net.maxsmr.mxstemplate.ui.fragment.AboutFragmentDirections
import net.maxsmr.mxstemplate.ui.fragment.FeedbackFragmentArgs
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    override val repo: CacheDataStoreRepository,
    state: SavedStateHandle
): BaseAboutViewModel(state) {

    override fun navigateToMarket(activity: Activity) {
        EntryPointAccessors.fromApplication(baseApplicationContext,
            EntryPointFeatureMobileServices::class.java)
            .marketIntentLauncher.startActivityMarketIntent(activity)
    }

    override fun navigateToFeedback() {
        navigate(NavigationAction.NavigationCommand.ToDirectionWithNavDirections(
            AboutFragmentDirections.actionToFeedbackFragment("") // TODO подставлять из BuildConfig
        ))
    }
}