package net.maxsmr.feature.download.ui.activity

import android.view.LayoutInflater
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.ui.components.activities.BaseDrawerNavigationActivity
import net.maxsmr.core.ui.databinding.LayoutHeaderNavigationViewBinding
import net.maxsmr.feature.download.ui.R

@AndroidEntryPoint
class DownloadDrawerActivity : BaseDrawerNavigationActivity() {

    override val navigationGraphResId: Int = R.navigation.navigation_download

    override val menuResId: Int = R.menu.menu_navigation_download

    override val headerView: View by lazy {
        LayoutHeaderNavigationViewBinding.inflate(LayoutInflater.from(this)).apply {
            tvTitle.setText(R.string.download_feature_title)
        }.root
    }
}