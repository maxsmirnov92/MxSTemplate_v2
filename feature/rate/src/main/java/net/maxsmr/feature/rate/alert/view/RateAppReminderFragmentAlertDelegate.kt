package net.maxsmr.feature.rate.alert.view

import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.core.ui.view.alert.delegate.BaseFragmentViewAlertDelegate
import net.maxsmr.core.ui.view.alert.representation.asYesNoNeutralDialog
import net.maxsmr.feature.rate.RateAppReminderComponentDelegate
import net.maxsmr.feature.rate.RateAppReminderComponentDelegate.Companion.DIALOG_TAG_RATE_APP_REMINDER

class RateAppReminderFragmentAlertDelegate<VM : BaseViewModel>(
    override val fragment: BaseVmFragment<VM>,
    override val viewModel: VM,
    private val delegate: RateAppReminderComponentDelegate,
) : BaseFragmentViewAlertDelegate<VM>() {

    override fun handleAlertDialogs() {
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