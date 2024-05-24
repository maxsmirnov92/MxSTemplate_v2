package net.maxsmr.feature.download.ui.activity

import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.ui.components.activities.BaseDrawerNavigationActivity
import net.maxsmr.feature.download.ui.R

@AndroidEntryPoint
class DownloadDrawerActivity : BaseDrawerNavigationActivity() {

    override val navigationGraphResId: Int = R.navigation.navigation_download

    override fun setupNavigationView() {
        // Фрагменты из этого меню должны быть в графе!
        navigationView.inflateMenu(R.menu.menu_download)
        navigationView.inflateHeaderView(R.layout.layout_header_download)
        super.setupNavigationView()
    }
}