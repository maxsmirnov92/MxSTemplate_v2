package net.maxsmr.mxstemplate.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.ui.components.activities.BaseBottomNavigationActivity
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.mxstemplate.App
import net.maxsmr.mxstemplate.R
import javax.inject.Inject

@AndroidEntryPoint
class MainBottomActivity : BaseBottomNavigationActivity() {

    override val navigationGraphResId: Int = R.navigation.navigation_main

    override val topLevelDestinationIds =
        setOf(
            R.id.navigationDownloads,
            R.id.navigationAddressSorter,
            R.id.navigationWebView,
            R.id.navigationSettings,
            R.id.navigationAbout
        )

    override val navigationMenuResId: Int = R.menu.menu_navigation_main

    override val backPressedOverrideMode: BackPressedMode
        // логику можно применять только если не webView и поверх текущего не было других navigate
        get() = if (currentNavDestinationId != R.id.navigationWebView
                && topLevelDestinationIds.contains(currentNavDestinationId)
        ) {
            BackPressedMode.PRESS_TWICE_CURRENT
        } else {
            BackPressedMode.NO_CHANGE
        }

    @Inject
    lateinit var settingsRepo: SettingsDataStoreRepository

//    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
//        super.onDestinationChanged(controller, destination, arguments)
//        bottomNavigationView.isVisible = destination.id != R.id.navigationWebView
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigateWithGraphFragmentsFromCaller()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateWithGraphFragmentsFromCaller()
    }

    override fun setupBottomNavigationView() {
        super.setupBottomNavigationView()
        bottomNavigationView.setOnItemSelectedListener { item ->
            navController.navigateWithGraphFragments(
                item,
                lifecycleScope,
                settingsRepo,
                currentNavFragment
            )
        }
    }

    private fun navigateWithGraphFragmentsFromCaller() {
        callerClass?.let {
            navController.navigateWithGraphFragmentsFromCaller(
                it,
                lifecycleScope,
                settingsRepo,
                currentNavFragment
            )
        }
    }
}