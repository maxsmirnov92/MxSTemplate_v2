package net.maxsmr.feature.about.alert.view

import androidx.fragment.app.Fragment
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.view.alert.ViewFragmentAlertDelegate
import net.maxsmr.core.ui.view.alert.representation.asCommonWrapBottomSheetDialog
import net.maxsmr.feature.about.ReleaseNotesComponentDelegate.Companion.DIALOG_TAG_RELEASE_NOTES

class ReleaseNotesFragmentAlertDelegate<VM: BaseViewModel>(
    fragment: Fragment,
    viewModel: VM,
): ViewFragmentAlertDelegate<VM>(fragment, viewModel) {

    override fun handleCommonAlertDialogs() {
        super.handleCommonAlertDialogs()
        bindAlertDialog(DIALOG_TAG_RELEASE_NOTES) {
            it.asCommonWrapBottomSheetDialog(fragment.requireContext(), false)
        }
    }
}