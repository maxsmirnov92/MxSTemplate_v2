package net.maxsmr.feature.preferences.ui

import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.core.ui.view.alert.delegate.BaseFragmentViewAlertDelegate
import net.maxsmr.core.ui.view.alert.representation.asYesNoNeutralDialog
import net.maxsmr.feature.preferences.ui.SettingsViewModel.Companion.DIALOG_TAG_CONFIRM_EXIT

class SettingsFragmentAlertDelegate(
    override val fragment: BaseVmFragment<SettingsViewModel>,
    override val viewModel: SettingsViewModel,
) : BaseFragmentViewAlertDelegate<SettingsViewModel>() {

    override fun handleAlertDialogs() {
        bindAlertDialog(DIALOG_TAG_CONFIRM_EXIT) {
            it.asYesNoNeutralDialog(context)
        }
    }
}