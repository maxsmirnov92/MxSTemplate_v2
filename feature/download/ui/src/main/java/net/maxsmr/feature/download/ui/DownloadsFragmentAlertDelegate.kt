package net.maxsmr.feature.download.ui

import androidx.fragment.app.Fragment
import net.maxsmr.core.ui.view.alert.delegate.ViewFragmentAlertDelegate
import net.maxsmr.core.ui.view.alert.representation.asOkDialog
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.download.data.DownloadsViewModel.Companion.DIALOG_TAG_FAILED_ADD_TO_QUEUE
import net.maxsmr.feature.download.data.DownloadsViewModel.Companion.DIALOG_TAG_FAILED_START

class DownloadsFragmentAlertDelegate(
    fragment: Fragment,
    viewModel: DownloadsViewModel
): ViewFragmentAlertDelegate<DownloadsViewModel>(fragment, viewModel) {

    override fun handleCommonAlertDialogs() {
        super.handleCommonAlertDialogs()
        bindAlertDialog(DIALOG_TAG_FAILED_ADD_TO_QUEUE) {
            it.asOkDialog(context)
        }
        bindAlertDialog(DIALOG_TAG_FAILED_START) {
            it.asOkDialog(context)
        }
    }
}