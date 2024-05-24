package net.maxsmr.feature.download.ui

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.NavArgs
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import net.maxsmr.commonutils.gui.hideKeyboard
import net.maxsmr.core.android.base.delegates.viewBinding
import net.maxsmr.core.ui.components.fragments.BaseNavigationFragment
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.download.ui.adapter.DownloadsPagerAdapter
import net.maxsmr.feature.download.ui.databinding.FragmentDownloadsPagerBinding
import net.maxsmr.permissionchecker.PermissionsHelper
import javax.inject.Inject
import kotlin.reflect.KClass

@AndroidEntryPoint
class DownloadsPagerFragment : BaseNavigationFragment<DownloadsViewModel, NavArgs>() {

    override val layoutId: Int = R.layout.fragment_downloads_pager

    override val viewModel: DownloadsViewModel by activityViewModels()

    override val argsClass: KClass<NavArgs>? = null

    @Inject
    override lateinit var permissionsHelper: PermissionsHelper

    private val binding by viewBinding(FragmentDownloadsPagerBinding::bind)

    private val pageChangeCallback: ViewPager2.OnPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            hideKeyboard()
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
        viewModel: DownloadsViewModel,
    ) {
        super.onViewCreated(view, savedInstanceState, viewModel)
        with(binding) {
            val pagerAdapter = DownloadsPagerAdapter(this@DownloadsPagerFragment)
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

    private fun hideKeyboard() {
        hideKeyboard(requireActivity(), flags = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED)
    }
}