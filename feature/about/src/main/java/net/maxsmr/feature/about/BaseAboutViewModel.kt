package net.maxsmr.feature.about

import android.app.Activity
import android.content.Context
import androidx.annotation.CallSuper
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.toRepresentation
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.rate.dialog.RateDialog
import net.maxsmr.feature.rate.dialog.RateDialog.Companion.RATE_THRESHOLD_DEFAULT

abstract class BaseAboutViewModel(state: SavedStateHandle): BaseHandleableViewModel(state) {

    protected abstract val repo: CacheDataStoreRepository

    override fun handleAlerts(context: Context, delegate: AlertFragmentDelegate<*>) {
        super.handleAlerts(context, delegate)
        delegate.bindAlertDialog(DIALOG_TAG_RATE_APP) {
            RateDialog(delegate.fragment, it, object : RateDialog.RateListener {
                override fun onRateSelected(rating: Int) {
                    if (rating >= RATE_THRESHOLD_DEFAULT) {
                        navigateToMarket(delegate.fragment.requireActivity())
                    } else {
                        navigateToFeedback()
                    }
                }
            }).toRepresentation()
        }
    }

    abstract fun navigateToMarket(activity: Activity)

    abstract fun navigateToFeedback()

    @CallSuper
    open fun onRateAppSelected() {
        viewModelScope.launch {
            repo.setAppRated()
        }
        AlertBuilder(DIALOG_TAG_RATE_APP)
            .setTitle(net.maxsmr.feature.rate.R.string.rate_dialog_app_title)
            .setAnswers(
                Alert.Answer(net.maxsmr.feature.rate.R.string.rate_dialog_app_button_positive),
                Alert.Answer(net.maxsmr.feature.rate.R.string.rate_dialog_app_button_negative),
            )
            .build()
    }

    companion object {

        const val DIALOG_TAG_RATE_APP = "rate_app"
    }
}