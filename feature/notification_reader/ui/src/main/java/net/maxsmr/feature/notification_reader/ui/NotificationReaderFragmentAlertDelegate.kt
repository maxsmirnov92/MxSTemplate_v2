package net.maxsmr.feature.notification_reader.ui

import androidx.fragment.app.Fragment
import net.maxsmr.core.ui.view.alert.delegate.ViewFragmentAlertDelegate

class NotificationReaderFragmentAlertDelegate(
    fragment: Fragment,
    viewModel: NotificationReaderViewModel
): ViewFragmentAlertDelegate<NotificationReaderViewModel>(fragment, viewModel)