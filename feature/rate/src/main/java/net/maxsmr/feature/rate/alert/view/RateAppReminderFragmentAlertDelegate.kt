package net.maxsmr.feature.rate.alert.view

import androidx.fragment.app.Fragment
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.view.alert.delegate.ViewFragmentAlertDelegate
import net.maxsmr.core.ui.view.alert.representation.asYesNoNeutralDialog
import net.maxsmr.feature.rate.RateAppReminderComponentDelegate
import net.maxsmr.feature.rate.RateAppReminderComponentDelegate.Companion.DIALOG_TAG_RATE_APP_REMINDER

class RateAppReminderFragmentAlertDelegate<VM : BaseViewModel>(
    private val delegate: RateAppReminderComponentDelegate,
    fragment: Fragment,
    viewModel: VM,
) : ViewFragmentAlertDelegate<VM>(fragment, viewModel) {

    override fun handleCommonAlertDialogs() {
        super.handleCommonAlertDialogs()
        bindAlertDialog(DIALOG_TAG_RATE_APP_REMINDER) {
            it.asYesNoNeutralDialog(
                fragment.requireContext(),
                onCancel = {
                    delegate.onCancelReminder()
                },
                onClick = { b ->
                    delegate.onConfirmReminder(b)
                }
            )
        }
    }
}