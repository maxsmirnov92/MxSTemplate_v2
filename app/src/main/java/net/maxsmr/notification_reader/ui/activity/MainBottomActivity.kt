package net.maxsmr.notification_reader.ui.activity

import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.ui.components.activities.BaseBottomNavigationActivity
import net.maxsmr.notification_reader.R

@AndroidEntryPoint
class MainBottomActivity : BaseBottomNavigationActivity() {

    override val navigationGraphResId: Int = R.navigation.navigation_main

    override val topLevelDestinationIds =
        setOf(
            R.id.navigationNotificationReader,
            R.id.navigationSettings,
        )

    override val navigationMenuResId: Int = R.menu.menu_navigation_main

    override val backPressedOverrideMode: BackPressedMode = BackPressedMode.PRESS_TWICE_LAST

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigateWithGraphFragmentsFromCaller()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navigateWithGraphFragmentsFromCaller()
    }

    override fun setupBottomNavigationView() {
        super.setupBottomNavigationView()
        bottomNavigationView.setOnItemSelectedListener { item ->
            navController.navigateWithGraphFragments(
                item,
                currentNavFragment
            )
        }
    }

    private fun navigateWithGraphFragmentsFromCaller() {
        callerClass?.let {
            navController.navigateWithGraphFragmentsFromCaller(
                it,
                currentNavFragment
            )
        }
    }
}