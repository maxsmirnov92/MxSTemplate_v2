package net.maxsmr.feature.about.alert.view

import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.core.ui.view.alert.delegate.BaseFragmentViewAlertDelegate
import net.maxsmr.core.ui.view.alert.representation.asCommonWrapBottomSheetDialog
import net.maxsmr.feature.about.ReleaseNotesComponentDelegate.Companion.DIALOG_TAG_RELEASE_NOTES

class ReleaseNotesFragmentAlertDelegate<VM : BaseViewModel>(
    override val fragment: BaseVmFragment<VM>,
    override val viewModel: VM,
) : BaseFragmentViewAlertDelegate<VM>() {

    override fun handleAlertDialogs() {
        bindAlertDialog(DIALOG_TAG_RELEASE_NOTES) {
            it.asCommonWrapBottomSheetDialog(context, false)
        }
    }
}