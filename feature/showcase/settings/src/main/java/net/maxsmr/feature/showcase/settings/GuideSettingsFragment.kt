package net.maxsmr.feature.showcase.settings

import android.view.MenuItem
import android.view.View
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.maxsmr.core.ui.components.IFragmentDelegate
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.ui.SettingsFragment
import net.maxsmr.feature.showcase.GuideFragmentDelegate
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType
import javax.inject.Inject

@AndroidEntryPoint
class GuideSettingsFragment : SettingsFragment(), GuideFragmentDelegate.GuideChecker {

    override val menuResId: Int = R.menu.menu_settings

    private val guideDelegate: GuideFragmentDelegate by lazy {
            GuideFragmentDelegate(
                this@GuideSettingsFragment,
                viewModel,
                this@GuideSettingsFragment,
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

    companion object {

        // TODO move
        private fun Toolbar.findOverflowButton(): View? {
            for (i in 0 until childCount) {
                val child: View = getChildAt(i)
                if (child is ActionMenuView) {
                    for (j in 0 until child.childCount) {
                        val innerChild: View = child.getChildAt(j)
                        if (innerChild.javaClass.simpleName == "OverflowMenuButton") {
                            // Это кнопка с тремя точками
                            return innerChild
                        }
                    }
                }
            }
            return null
        }

        private fun Toolbar.findActionMenuItemView(@IdRes actionId: Int): View? {
            for (i in 0 until childCount) {
                val child: View = getChildAt(i)
                if (child is ActionMenuView) {
                    for (j in 0 until child.childCount) {
                        val innerChild: View = child.getChildAt(j)
                        if (innerChild.id == actionId) {
                            return innerChild
                        }
                    }
                }
            }
            return null
        }
    }
}