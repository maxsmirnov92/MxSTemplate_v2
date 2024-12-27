package net.maxsmr.feature.rate.alert.view

import androidx.fragment.app.Fragment
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.view.alert.representation.toRepresentation
import net.maxsmr.core.ui.view.alert.ViewFragmentAlertDelegate
import net.maxsmr.feature.rate.BaseRateAppComponentDelegate
import net.maxsmr.feature.rate.BaseRateAppComponentDelegate.Companion.DIALOG_TAG_RATE_APP
import net.maxsmr.feature.rate.alert.view.dialog.RateDialog

class RateAppFragmentAlertDelegate<VM: BaseViewModel>(
    private val delegate: BaseRateAppComponentDelegate,
    fragment: Fragment,
    viewModel: VM,
): ViewFragmentAlertDelegate<VM>(fragment, viewModel) {

    override fun handleCommonAlertDialogs() {
        super.handleCommonAlertDialogs()
        bindAlertDialog(DIALOG_TAG_RATE_APP) {
            RateDialog(fragment, it) { rating ->
                delegate.onRateAppSelected(rating)
            }.toRepresentation()
        }
    }
}