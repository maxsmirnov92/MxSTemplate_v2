package net.maxsmr.mxstemplate.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.ui.components.activities.BaseDrawerNavigationActivity
import net.maxsmr.core.ui.view.databinding.LayoutHeaderNavigationViewBinding
import net.maxsmr.mxstemplate.R

@AndroidEntryPoint
class MainDrawerActivity : BaseDrawerNavigationActivity() {

    override val navigationGraphResId: Int = R.navigation.navigation_main

    override val menuResId: Int = R.menu.menu_navigation_main

    override val headerView: View by lazy {
        LayoutHeaderNavigationViewBinding.inflate(LayoutInflater.from(this)).apply {
            tvTitle.setText(R.string.app_name)
            tvSubHeader.isVisible = false
        }.root
    }

    override val backPressedOverrideMode: BackPressedMode = BackPressedMode.PRESS_TWICE_LAST

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navigateWithGraphFragmentsFromCaller()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navigateWithGraphFragmentsFromCaller()
    }

    override fun setupNavigationView() {
        super.setupNavigationView()
        navigationView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawers()
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