package net.maxsmr.mxstemplate.ui.activity

import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.maxsmr.core.ui.components.activities.BaseBottomNavigationActivity
import net.maxsmr.feature.download.ui.DownloadsPagerFragmentDirections
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.feature.webview.ui.WebViewCustomizer
import net.maxsmr.mxstemplate.R
import javax.inject.Inject

@AndroidEntryPoint
class MainBottomActivity : BaseBottomNavigationActivity() {

    override val navigationGraphResId: Int = R.navigation.navigation_main

    override val topLevelDestinationIds = setOf(R.id.navigationDownloads, R.id.navigationWebView, R.id.navigationSettings, R.id.navigationAbout)

    override val navigationMenuResId: Int = R.menu.menu_navigation_main

    override val backPressedOverrideMode: BackPressedMode get() = if (currentNavDestinationId != R.id.navigationWebView) {
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

    override fun setupBottomNavigationView() {
        super.setupBottomNavigationView()
        bottomNavigationView.setOnItemSelectedListener { item ->
            return@setOnItemSelectedListener if (item.itemId != navController.currentDestination?.id) {
                if (item.itemId == R.id.navigationWebView) {
                    lifecycleScope.launch {
                        // FIXME за счёт этого криво будет работать navigateUp с других фрагментов на WebView
                        navController.navigate(
                            DownloadsPagerFragmentDirections.actionToWebViewFragment(
                                WebViewCustomizer.Builder()
                                    .setUrl(settingsRepo.getSettings().startPageUrl)
                                    .setCanInputUrls(true)
                                    .build()
                            )
                        )
                    }
                } else {
                    navController.navigate(item.itemId)
                }
                true
            } else {
                false
            }
        }
    }
}