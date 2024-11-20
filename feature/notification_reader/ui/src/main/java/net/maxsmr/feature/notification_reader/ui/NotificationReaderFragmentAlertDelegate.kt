package net.maxsmr.feature.notification_reader.ui

import androidx.fragment.app.Fragment
import net.maxsmr.core.ui.view.alert.delegate.ViewFragmentAlertDelegate
import net.maxsmr.core.ui.view.alert.representation.asOkDialog
import net.maxsmr.feature.notification_reader.ui.NotificationReaderViewModel.Companion.DIALOG_TAG_IMPORT_FAILED

class NotificationReaderFragmentAlertDelegate(
    fragment: Fragment,
    viewModel: NotificationReaderViewModel
): ViewFragmentAlertDelegate<NotificationReaderViewModel>(fragment, viewModel) {

    override fun handleCommonAlertDialogs() {
        super.handleCommonAlertDialogs()
        bindAlertDialog(DIALOG_TAG_IMPORT_FAILED) {
            it.asOkDialog(context)
        }
    }
}