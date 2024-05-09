package net.maxsmr.feature.download.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.core.android.base.alert.AlertHandler
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.download.ui.adapter.DownloadsPagerAdapter
import net.maxsmr.feature.download.ui.databinding.FragmentDownloadsPagerBinding
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject

@AndroidEntryPoint
class DownloadsPagerFragment: BaseVmFragment<DownloadsViewModel>() {

    override val layoutId: Int = R.layout.fragment_downloads_pager

    override val viewModel: DownloadsViewModel by viewModels()

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    private val binding by viewBinding(FragmentDownloadsPagerBinding::bind)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
        viewModel: DownloadsViewModel,
        alertHandler: AlertHandler,
    ) {
        with(binding) {
            val pagerAdapter = DownloadsPagerAdapter(this@DownloadsPagerFragment)
            viewPagerFragments.adapter = pagerAdapter
            TabLayoutMediator(tabLayout, viewPagerFragments) { tab, position ->
                tab.text = pagerAdapter.getTitle(position)
            }.attach()
            viewPagerFragments.isUserInputEnabled = false
        }

        viewModel.handleEvents(this)
    }
}