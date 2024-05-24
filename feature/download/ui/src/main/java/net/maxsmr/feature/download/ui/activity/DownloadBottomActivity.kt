package net.maxsmr.feature.download.ui.activity

import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.ui.components.activities.BaseBottomNavigationActivity
import net.maxsmr.feature.download.ui.R

@AndroidEntryPoint
class DownloadBottomActivity : BaseBottomNavigationActivity() {

    override val navigationGraphResId: Int = R.navigation.navigation_download

    override val topLevelDestinationIds = setOf(/*R.id.navigationMain,*/ R.id.navigationDownloads, R.id.navigationSettings)

    override val menuResId: Int = R.menu.menu_download
}