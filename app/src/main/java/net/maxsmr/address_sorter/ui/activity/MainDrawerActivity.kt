package net.maxsmr.address_sorter.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.address_sorter.App
import net.maxsmr.address_sorter.R
import net.maxsmr.core.android.baseApplicationContext
import net.maxsmr.core.ui.components.activities.BaseDrawerNavigationActivity
import net.maxsmr.core.ui.databinding.LayoutHeaderNavigationViewBinding
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import javax.inject.Inject

@AndroidEntryPoint
class MainDrawerActivity : BaseDrawerNavigationActivity() {

    override val navigationGraphResId: Int = R.navigation.navigation_main

    override val menuResId: Int = R.menu.menu_navigation_main

    override val headerView: View by lazy {
        LayoutHeaderNavigationViewBinding.inflate(LayoutInflater.from(this)).apply {
            ivHeader.setImageResource(net.maxsmr.feature.address_sorter.ui.R.drawable.ic_address_sorter)
            tvTitle.setText(R.string.app_name)
            tvTitle.setTextColor(
                ContextCompat.getColor(
                    this@MainDrawerActivity,
                    net.maxsmr.core.ui.R.color.textColorPrimary
                )
            )
            tvSubHeader.isVisible = false
        }.root
    }

    override val backPressedOverrideMode: BackPressedMode = BackPressedMode.PRESS_TWICE_LAST

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
}