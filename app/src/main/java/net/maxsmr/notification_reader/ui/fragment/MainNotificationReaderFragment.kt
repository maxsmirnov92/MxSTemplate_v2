package net.maxsmr.notification_reader.ui.fragment

import android.os.Bundle
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.core.ui.views.ViewClickDelegate
import net.maxsmr.feature.notification_reader.ui.NotificationReaderFragment
import net.maxsmr.feature.notification_reader.ui.NotificationReaderViewModel
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainNotificationReaderFragment: NotificationReaderFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: NotificationReaderViewModel) {
        super.onViewCreated(view, savedInstanceState, viewModel)
        ViewClickDelegate(binding.toolbar, 25, TimeUnit.SECONDS.toMillis(2)).setOnClickListener {
            viewModel.navigate(
                NavigationAction.NavigationCommand.ToDirectionWithNavDirections(
                    MainNotificationReaderFragmentDirections.actionToSettings()
                )
            )
        }
    }
}