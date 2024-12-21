package net.maxsmr.feature.download.ui

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import net.maxsmr.commonutils.gui.hideKeyboard
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.download.ui.adapter.DownloadsPagerAdapter
import net.maxsmr.feature.download.ui.databinding.FragmentDownloadsPagerBinding

abstract class BaseDownloadsPagerFragment : BaseNavigationFragment<DownloadsViewModel>() {

    override val layoutId: Int = R.layout.fragment_downloads_pager

    override val viewModel: DownloadsViewModel by activityViewModels()

    private val binding by viewBinding(FragmentDownloadsPagerBinding::bind)

    private val pageChangeCallback: ViewPager2.OnPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            requireActivity().hideKeyboard(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
        viewModel: DownloadsViewModel,
    ) {
        super.onViewCreated(view, savedInstanceState, viewModel)
        with(binding) {
            val pagerAdapter = DownloadsPagerAdapter(this@BaseDownloadsPagerFragment)
            viewPagerFragments.adapter = pagerAdapter
            TabLayoutMediator(tabLayout, viewPagerFragments) { tab, position ->
                tab.text = pagerAdapter.getTitle(position)
            }.attach()
            viewPagerFragments.isUserInputEnabled = false
            viewPagerFragments.registerOnPageChangeCallback(pageChangeCallback)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewPagerFragments.unregisterOnPageChangeCallback(pageChangeCallback)
    }
}