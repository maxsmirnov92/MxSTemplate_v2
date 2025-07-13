package net.maxsmr.feature.download.ui

import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.core.ui.view.alert.delegate.BaseFragmentViewAlertDelegate
import net.maxsmr.core.ui.view.alert.representation.asOkDialog
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.download.data.DownloadsViewModel.Companion.DIALOG_TAG_FAILED_ADD_TO_QUEUE
import net.maxsmr.feature.download.data.DownloadsViewModel.Companion.DIALOG_TAG_FAILED_START

class DownloadsFragmentAlertDelegate(
    override val fragment: BaseVmFragment<*>,
    override val viewModel: DownloadsViewModel
): BaseFragmentViewAlertDelegate<DownloadsViewModel>() {

    override fun handleAlertDialogs() {
        bindAlertDialog(DIALOG_TAG_FAILED_ADD_TO_QUEUE) {
            it.asOkDialog(context)
        }
        bindAlertDialog(DIALOG_TAG_FAILED_START) {
            it.asOkDialog(context)
        }
    }
}