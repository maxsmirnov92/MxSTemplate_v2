package net.maxsmr.feature.camera.ui

import android.content.res.Configuration
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.ui.components.activities.BaseNavigationActivity
import net.maxsmr.feature.camera.R

@AndroidEntryPoint
class CameraActivity: BaseNavigationActivity() {

    override val navigationGraphResId: Int = R.navigation.navigation_camera

    override val backPressedOverrideMode: BackPressedMode = BackPressedMode.PRESS_TWICE_LAST
}