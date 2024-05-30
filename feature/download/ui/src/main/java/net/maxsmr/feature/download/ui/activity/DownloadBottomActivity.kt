package net.maxsmr.feature.download.ui.activity

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.ui.components.activities.BaseBottomNavigationActivity
import net.maxsmr.feature.download.ui.R

@AndroidEntryPoint
class DownloadBottomActivity : BaseBottomNavigationActivity() {

    override val navigationGraphResId: Int = R.navigation.navigation_download

    override val topLevelDestinationIds = setOf(/*R.id.navigationMain,*/ R.id.navigationDownloads, R.id.navigationSettings)

    override val navigationMenuResId: Int = R.menu.menu_navigation_download

    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        super.onDestinationChanged(controller, destination, arguments)
        bottomNavigationView.isVisible = destination.id != R.id.navigationWebView
    }
}