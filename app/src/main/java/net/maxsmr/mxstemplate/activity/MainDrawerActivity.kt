package net.maxsmr.mxstemplate.activity

import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.ui.components.activities.BaseDrawerNavigationActivity
import net.maxsmr.mxstemplate.R

@AndroidEntryPoint
class MainDrawerActivity : BaseDrawerNavigationActivity() {

    override val navigationGraphResId: Int = R.navigation.navigation_main

    override fun setupNavigationView() {
        // Фрагменты из этого меню должны быть в графе!
        navigationView.inflateMenu(R.menu.menu_main)
        navigationView.inflateHeaderView(R.layout.layout_header_main)
        super.setupNavigationView()
    }
}