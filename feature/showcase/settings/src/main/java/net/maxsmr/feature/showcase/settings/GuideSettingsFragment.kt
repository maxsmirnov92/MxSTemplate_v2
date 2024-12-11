package net.maxsmr.feature.showcase.settings

import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.maxsmr.commonutils.gui.isFullyVisible
import net.maxsmr.commonutils.gui.scrollToView
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.ui.SettingsFragment
import net.maxsmr.feature.showcase.GuideFragmentDelegate
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType
import javax.inject.Inject


@AndroidEntryPoint
class GuideSettingsFragment : SettingsFragment(), GuideFragmentDelegate.GuideChecker {

    override val menuResId: Int = R.menu.menu_settings_guide

    private val guideDelegate: GuideFragmentDelegate by lazy {
        GuideFragmentDelegate(
            this@GuideSettingsFragment,
            viewModel,
            this@GuideSettingsFragment,
            onNextListener = { item, _ ->
                if (!item.view.isFullyVisible()) {
                    binding.svSettings.scrollToView(
                        item.view,
                        isVertically = true,
                        // целевая view внизу
                        alignToBottomOrRight = true,
                        // не использовать smooth, т.к. GuideView возьмёт неправильные координаты
                        smoothScroll = false
                    )
                }
            }
        )
    }

    override var isCompleted: Boolean
        get() = runBlocking { cacheRepo.isTutorialCompeted() }
        set(value) {
            lifecycleScope.launch {
                cacheRepo.setTutorialCompleted(value)
                if (isGuidePendingStart) {
                    guideDelegate.doStart()
                    isGuidePendingStart = false
                }
            }
        }

    @Inject
    lateinit var cacheRepo: CacheDataStoreRepository

    private var isGuidePendingStart = false

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return if (menuItem.itemId == R.id.actionStartGuide) {
            isGuidePendingStart = true
            isCompleted = false
            true
        } else {
            super.onMenuItemSelected(menuItem)
        }
    }

    override fun createFragmentDelegates(): List<IFragmentDelegate> {
        return listOf(guideDelegate)
    }

    private fun View.settingsGuideItem(
        key: String,
        @StringRes
        textRes: Int,
        @StringRes
        titleRes: Int? = null,
    ) = GuideFragmentDelegate.GuideItem(key, this) {
        titleRes?.let {
            setTitle(getString(titleRes))
        }
        setContentText(getString(textRes))
        setDismissType(DismissType.selfView)
    }
}