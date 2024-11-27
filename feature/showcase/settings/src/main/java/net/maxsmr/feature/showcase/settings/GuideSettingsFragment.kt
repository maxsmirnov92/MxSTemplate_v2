package net.maxsmr.feature.showcase.settings

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.maxsmr.commonutils.gui.findOverflowButton
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

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateMenu(menu, inflater)
        // срабатывает 2 раза...
        with(binding) {
            requireView().post {
                if (!guideDelegate.wasStarted) {
                    // заполнение итемов пришлось перенести сюда
                    // из-за неготовности OverflowMenuButton
                    guideDelegate.items = mutableListOf(
                        toolbar.settingsGuideItem(
                            "root",
                            R.string.showcase_settings_description_summary,
                        ),
                        tilNotificationsUrl.settingsGuideItem(
                            "notificationsUrl",
                            R.string.showcase_settings_description_notifications_url
                        ),
                        tilPackageListUrl.settingsGuideItem(
                            "packageListUrl",
                            R.string.showcase_settings_description_package_list_url
                        ),
                        switchWhitePackageList.settingsGuideItem(
                            "isWhitePackageList",
                            R.string.showcase_settings_description_is_white_package_list
                        ),
                        tilFailedNotificationsWatcherInterval.settingsGuideItem(
                            "failedNotificationsWatcherInterval",
                            R.string.showcase_settings_description_failed_notifications_watcher_interval
                        ),
                        tilConnectTimeout.settingsGuideItem(
                            "connectTimeout",
                            R.string.showcase_settings_description_connect_timeout
                        ),
                        switchLoadByWiFiOnly.settingsGuideItem(
                            "loadByWiFiOnly",
                            R.string.showcase_settings_description_load_by_wi_fi_only
                        ),
                        switchRetryOnConnectionFailure.settingsGuideItem(
                            "retryOnConnectionFailure",
                            R.string.showcase_settings_description_retry_on_connection_failure
                        ),
                        switchRetryDownloads.settingsGuideItem(
                            "retryDownloads",
                            R.string.showcase_settings_description_retry_downloads
                        ),
                        switchDisableNotifications.settingsGuideItem(
                            "disableNotifications",
                            R.string.showcase_settings_description_disable_notifications
                        )
                    ).apply {
                        if (switchCanDrawOverlays.isVisible) {
                            add(
                                switchCanDrawOverlays.settingsGuideItem(
                                    "canDrawOverlays",
                                    R.string.showcase_settings_description_can_draw_overlays
                                )
                            )
                        }
                        add(
                            (toolbar.findOverflowButton() ?: toolbar).settingsGuideItem(
                                "importApiKey",
                                R.string.showcase_settings_description_import_api_key
                            )
                        )
                    }
                }
            }
        }
    }

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