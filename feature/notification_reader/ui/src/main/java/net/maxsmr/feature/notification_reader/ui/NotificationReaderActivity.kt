package net.maxsmr.feature.notification_reader.ui

import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.ui.components.activities.BaseNavigationActivity

@AndroidEntryPoint
class NotificationReaderActivity: BaseNavigationActivity() {

    override val navigationGraphResId: Int = R.navigation.navigation_app_notification

    override val backPressedOverrideMode: BackPressedMode = BackPressedMode.FINISH_LAST
}