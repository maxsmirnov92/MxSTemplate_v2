package net.maxsmr.notification_reader.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.asLiveData
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.commonutils.live.observeOnce
import net.maxsmr.core.android.base.actions.NavigationAction
import net.maxsmr.feature.notification_reader.ui.NotificationReaderFragment
import net.maxsmr.feature.notification_reader.ui.NotificationReaderViewModel

@AndroidEntryPoint
class GuideNotificationReaderFragment: NotificationReaderFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?, viewModel: NotificationReaderViewModel) {
        super.onViewCreated(view, savedInstanceState, viewModel)
        cacheRepo.isTutorialCompleted.asLiveData().observeOnce(this) {
            if (!it) {
                viewModel.navigate(
                    NavigationAction.NavigationCommand.ToDirectionWithNavDirections(
                        GuideNotificationReaderFragmentDirections.actionToSettings()
                    )
                )
            }
        }
    }
}