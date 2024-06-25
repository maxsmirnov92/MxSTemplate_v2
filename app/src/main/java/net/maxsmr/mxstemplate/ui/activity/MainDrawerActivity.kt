package net.maxsmr.mxstemplate.ui.activity

import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.maxsmr.core.ui.components.activities.BaseDrawerNavigationActivity
import net.maxsmr.core.ui.databinding.LayoutHeaderNavigationViewBinding
import net.maxsmr.feature.download.ui.DownloadsPagerFragmentDirections
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.feature.webview.ui.WebViewCustomizer
import net.maxsmr.mxstemplate.R
import javax.inject.Inject

@AndroidEntryPoint
class MainDrawerActivity : BaseDrawerNavigationActivity() {

    override val navigationGraphResId: Int = R.navigation.navigation_main

    override val menuResId: Int = R.menu.menu_navigation_main

    override val headerView: View by lazy {
        LayoutHeaderNavigationViewBinding.inflate(LayoutInflater.from(this)).apply {
            tvTitle.setText(R.string.app_name)
        }.root
    }

    override val backPressedOverrideMode: BackPressedMode
        get() = if (currentNavDestinationId != R.id.navigationWebView) {
            BackPressedMode.PRESS_TWICE_LAST
        } else {
            BackPressedMode.NO_CHANGE
        }

    @Inject
    lateinit var settingsRepo: SettingsDataStoreRepository

    override fun setupNavigationView() {
        super.setupNavigationView()
        navigationView.setNavigationItemSelectedListener { item ->
            drawerLayout.closeDrawers()
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