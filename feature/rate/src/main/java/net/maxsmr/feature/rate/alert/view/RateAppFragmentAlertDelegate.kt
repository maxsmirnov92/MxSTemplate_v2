package net.maxsmr.feature.rate.alert.view

import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.core.ui.view.alert.delegate.BaseFragmentViewAlertDelegate
import net.maxsmr.core.ui.view.alert.representation.toRepresentation
import net.maxsmr.feature.rate.BaseRateAppComponentDelegate
import net.maxsmr.feature.rate.BaseRateAppComponentDelegate.Companion.DIALOG_TAG_RATE_APP
import net.maxsmr.feature.rate.alert.view.dialog.RateDialog

class RateAppFragmentAlertDelegate<VM : BaseViewModel>(
    override val fragment: BaseVmFragment<VM>,
    override val viewModel: VM,
    private val delegate: BaseRateAppComponentDelegate,
) : BaseFragmentViewAlertDelegate<VM>() {

    override fun handleAlertDialogs() {
        bindAlertDialog(DIALOG_TAG_RATE_APP) {
            RateDialog(fragment, it) { rating ->
                delegate.onRateAppSelected(rating)
            }.toRepresentation()
        }
    }
}