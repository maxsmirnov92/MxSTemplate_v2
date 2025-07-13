package net.maxsmr.feature.download.ui

import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.core.ui.view.alert.delegate.BaseFragmentViewAlertDelegate
import net.maxsmr.core.ui.view.alert.representation.asYesNoDialog
import net.maxsmr.feature.download.ui.DownloadsStateViewModel.Companion.DIALOG_TAG_CANCEL_ALL
import net.maxsmr.feature.download.ui.DownloadsStateViewModel.Companion.DIALOG_TAG_CLEAR_QUEUE
import net.maxsmr.feature.download.ui.DownloadsStateViewModel.Companion.DIALOG_TAG_DELETE_IF_SUCCESS
import net.maxsmr.feature.download.ui.DownloadsStateViewModel.Companion.DIALOG_TAG_RETRY_IF_SUCCESS

class DownloadsStateFragmentAlertDelegate(
    override val fragment: BaseVmFragment<DownloadsStateViewModel>,
    override val viewModel: DownloadsStateViewModel,
) : BaseFragmentViewAlertDelegate<DownloadsStateViewModel>() {

    override fun handleAlertDialogs() {
        bindAlertDialog(DIALOG_TAG_CLEAR_QUEUE) {
            it.asYesNoDialog(context)
        }
        bindAlertDialog(DIALOG_TAG_CANCEL_ALL) {
            it.asYesNoDialog(context)
        }
        bindAlertDialog(DIALOG_TAG_RETRY_IF_SUCCESS) {
            it.asYesNoDialog(context)
        }
        bindAlertDialog(DIALOG_TAG_DELETE_IF_SUCCESS) {
            it.asYesNoDialog(context)
        }
    }
}