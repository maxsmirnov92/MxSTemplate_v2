package net.maxsmr.feature.preferences.ui

import android.provider.Settings
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.map
import net.maxsmr.commonutils.live.observeOnce
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_BATTERY_OPTIMIZATION
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository

fun BaseViewModel.doOnBatteryOptimizationWithPostNotificationsAsk(
    fragment: BaseVmFragment<*>,
    cacheRepo: CacheDataStoreRepository,
    settingsRepo: SettingsDataStoreRepository,
    targetAction: () -> Unit
) {
    cacheRepo.doOnBatteryOptimizationAsk(this, fragment.requireContext(), DIALOG_TAG_BATTERY_OPTIMIZATION) {
        settingsRepo.settingsFlow.map { it.disableNotifications }.asLiveData().observeOnce(this) {
            // post_notifications не является обязательным для работы сервиса
            if (!it) {
                cacheRepo.doOnPostNotificationPermissionResult(fragment, false) {
                    targetAction()
                }
            } else {
                targetAction()
            }
        }
    }
}

fun BaseViewModel.doOnCanDrawOverlaysAsked(
    fragment: BaseVmFragment<*>,
    cacheRepo: CacheDataStoreRepository,
    settingsRepo: SettingsDataStoreRepository,
    targetAction: (Boolean) -> Unit
) {
    settingsRepo.settingsFlow.map { it.canDrawOverlays }.asLiveData().observeOnce(this) {
        if (it && !Settings.canDrawOverlays(fragment.requireContext())) {
            cacheRepo.doOnCanDrawOverlaysAsked(fragment, targetAction)
        }
    }
}