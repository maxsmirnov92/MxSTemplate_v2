package net.maxsmr.mxstemplate.activity

import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.ui.components.activities.BaseBottomNavigationActivity
import net.maxsmr.mxstemplate.R

@AndroidEntryPoint
class MainBottomActivity : BaseBottomNavigationActivity() {

    override val navigationGraphResId: Int = R.navigation.navigation_main

    override val topLevelDestinationIds = setOf(/*R.id.navigationMain,*/ R.id.navigationDownloads, R.id.navigationSettings)

    override fun setupBottomNavigationView() {
        bottomNavigationView.inflateMenu(R.menu.menu_main)
        super.setupBottomNavigationView()
    }
}