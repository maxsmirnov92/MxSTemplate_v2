package net.maxsmr.feature.download.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import net.maxsmr.feature.download.ui.DownloadsParamsFragment
import net.maxsmr.feature.download.ui.DownloadsStateFragment
import net.maxsmr.feature.download.ui.R

class DownloadsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val context = fragment.requireContext()

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> DownloadsParamsFragment()
        1 -> DownloadsStateFragment()
        else -> throw IllegalArgumentException("Incorrect position: $position")
    }

    fun getTitle(position: Int) = when(position) {
        0 -> context.getString(R.string.download_params_title)
        1 -> context.getString(R.string.download_state_title)
        else -> throw IllegalArgumentException("Incorrect position: $position")
    }
}