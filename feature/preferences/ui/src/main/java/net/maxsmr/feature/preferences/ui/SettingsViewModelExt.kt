package net.maxsmr.feature.preferences.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import net.maxsmr.commonutils.flow.observeLatest
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.BaseViewModel.Companion.DIALOG_TAG_BATTERY_OPTIMIZATION
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository

fun BaseViewModel.doOnBatteryOptimizationWithPostNotificationsAskIfNeeded(
    fragment: BaseVmFragment<*>,
    cacheRepo: CacheDataStoreRepository,
    settingsRepo: SettingsDataStoreRepository,
    targetAction: () -> Unit,
) {
    cacheRepo.doOnBatteryOptimizationAsk(this, fragment.requireContext(), DIALOG_TAG_BATTERY_OPTIMIZATION) {
        if (it) {
            doOnPostNotificationsAskIfNeeded(
                fragment,
                cacheRepo,
                settingsRepo,
                false, // post_notifications не является обязательным для работы сервиса
                targetAction
            )
        }
    }
}

fun BaseViewModel.doOnPostNotificationsAskIfNeeded(
    fragment: BaseVmFragment<*>,
    cacheRepo: CacheDataStoreRepository,
    settingsRepo: SettingsDataStoreRepository,
    onlyWhenGranted: Boolean,
    targetAction: () -> Unit,
) {
    settingsRepo.settingsFlow.map {
        it.disableNotifications
    }
        .take(1)
        .observeLatest(viewModelScope) {
            if (!it) {
                cacheRepo.doOnPostNotificationPermissionResult(fragment, onlyWhenGranted) {
                    targetAction()
                }
            } else {
                targetAction()
            }
        }
}