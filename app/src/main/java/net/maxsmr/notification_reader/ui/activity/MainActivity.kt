package net.maxsmr.notification_reader.ui.activity

import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.ui.components.activities.BaseNavigationActivity
import net.maxsmr.notification_reader.R

@AndroidEntryPoint
class MainActivity: BaseNavigationActivity() {

    override val navigationGraphResId: Int = R.navigation.navigation_main

    override val backPressedOverrideMode: BackPressedMode = BackPressedMode.PRESS_TWICE_LAST
}