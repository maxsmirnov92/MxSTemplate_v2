package net.maxsmr.feature.preferences.ui

import androidx.fragment.app.Fragment
import net.maxsmr.core.ui.view.alert.ViewFragmentAlertDelegate
import net.maxsmr.core.ui.view.alert.representation.asYesNoNeutralDialog
import net.maxsmr.feature.preferences.ui.SettingsViewModel.Companion.DIALOG_TAG_CONFIRM_EXIT

class SettingsFragmentAlertDelegate(
    fragment: Fragment,
    viewModel: SettingsViewModel,
) : ViewFragmentAlertDelegate<SettingsViewModel>(fragment, viewModel) {

    override fun handleCommonAlertDialogs() {
        super.handleCommonAlertDialogs()
        bindAlertDialog(DIALOG_TAG_CONFIRM_EXIT) {
            it.asYesNoNeutralDialog(context)
        }
    }

}