package net.maxsmr.feature.notification_reader.ui

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.datetime.Instant
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.feature.notification_reader.data.NotificationReaderListenerService
import net.maxsmr.feature.notification_reader.data.NotificationReaderRepository
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager.ManagerStartResult
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager.StartMode
import net.maxsmr.feature.notification_reader.ui.adapter.NotificationsAdapterData
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.feature.preferences.ui.doOnBatteryOptimizationWithPostNotificationsAsk
import javax.inject.Inject

@HiltViewModel
class NotificationReaderViewModel @Inject constructor(
    private val manager: NotificationReaderSyncManager,
    private val cacheRepo: CacheDataStoreRepository,
    private val settingsRepo: SettingsDataStoreRepository,
    repo: NotificationReaderRepository,
    state: SavedStateHandle,
) : BaseHandleableViewModel(state) {

    val serviceTargetState = MutableLiveData(true)

    val notificationsItems = repo.getNotifications().asLiveData().map { list ->
        list.map {
            NotificationsAdapterData(
                it.id,
                it.contentText,
                it.packageName,
                Instant.fromEpochMilliseconds(it.timestamp).toString(),
                it.status
            )
        }
    }

    fun doStartOrStop(fragment: BaseVmFragment<*>) {
        // post_notifications не является обязательным для работы сервиса,
        // но спрашиваем (при включённой настройке) чтобы нотификации от двух сервисов были
        doOnBatteryOptimizationWithPostNotificationsAsk(fragment, cacheRepo, settingsRepo) {
            with(fragment.requireContext()) {
                if (serviceTargetState.value == true) {
                    doStartWithHandleResult(this)
                } else {
                    doStopWithHandleResult(this, false)
                }
            }
        }
    }

    fun doStartWithHandleResult(context: Context): Boolean {
        return when (manager.doStart(context)) {
            ManagerStartResult.SERVICE_START_FAILED -> {
                showSnackbar(TextMessage(R.string.notification_reader_snack_cannot_start_service))
                false
            }
            ManagerStartResult.SETTINGS_NEEDED -> {
                showToast(TextMessage(R.string.notification_reader_toast_start_add_in_settings))
                false
            }
            ManagerStartResult.NOT_IN_FOREGROUND -> {
                false
            }

            ManagerStartResult.SUCCESS, ManagerStartResult.SUCCESS_PENDING -> true
        }
    }

    /**
     * @param navigateToSettings false если вернулись с экрана настроек (или в onResume)
     */
    fun doStopWithHandleResult(context: Context, navigateToSettings: Boolean): Boolean {
        // при возврате с экрана настроек, когда разрешение было отозвано,
        // можно попытаться остановить ещё раз
        return manager.doStop(context, navigateToSettings).also { isRunning ->
            if (isRunning && navigateToSettings) {
                showToast(TextMessage(R.string.notification_reader_toast_stop_remove_in_settings))
            }
        }
    }

    fun onToggleServiceTargetStateAction() {
        serviceTargetState.value = !isServiceRunning()
    }

    fun onDownloadPackageListAction() {
        if (!manager.doLaunchDownloadJobIfNeeded(
                    if (serviceTargetState.value == true) {
                        StartMode.JOBS_AND_SERVICE
                    } else {
                        StartMode.NONE
                    }
                )
        ) {
            showSnackbar(TextMessage(R.string.notification_reader_snack_download_package_list_not_started))
        }
    }

    fun isServiceRunning(): Boolean {
        return NotificationReaderListenerService.isRunning() /*&& serviceTargetState.value != false*/
    }
}