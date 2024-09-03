package net.maxsmr.mxstemplate.ui.activity

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
        setOf(R.id.navigationDownloads, R.id.navigationAddressSorter, R.id.navigationWebView, R.id.navigationSettings, R.id.navigationAbout)

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

    override val canUseFragmentDelegates: Boolean
        get() {
            val app = baseApplicationContext as App
            return app.isActivityFirstAndSingle(MainBottomActivity::class.java)
        }

    @Inject
    lateinit var settingsRepo: SettingsDataStoreRepository

//    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
//        super.onDestinationChanged(controller, destination, arguments)
//        bottomNavigationView.isVisible = destination.id != R.id.navigationWebView
//    }

    override fun setupBottomNavigationView() {
        super.setupBottomNavigationView()
        bottomNavigationView.setOnItemSelectedListener { item ->
            navController.navigateWithMenuFragments(
                item,
                lifecycleScope,
                settingsRepo,
                currentNavDestinationId,
                currentNavFragment
            )
        }
    }
}