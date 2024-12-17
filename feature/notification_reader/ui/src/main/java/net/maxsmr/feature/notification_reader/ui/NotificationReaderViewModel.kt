package net.maxsmr.feature.notification_reader.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.observeOnce
import net.maxsmr.commonutils.live.setValueIfNew
import net.maxsmr.core.android.base.delegates.persistableLiveData
import net.maxsmr.core.android.base.delegates.persistableValueInitial
import net.maxsmr.core.database.model.notification_reader.NotificationReaderEntity
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.feature.notification_reader.data.NotificationReaderListenerService
import net.maxsmr.feature.notification_reader.data.NotificationReaderRepository
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager.ManagerStartResult
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager.ManagerStopResult
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager.StartMode
import net.maxsmr.feature.notification_reader.ui.adapter.NotificationsAdapterData
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.feature.preferences.ui.doOnBatteryOptimizationWithPostNotificationsAsk
import java.io.Serializable
import javax.inject.Inject

@HiltViewModel
class NotificationReaderViewModel @Inject constructor(
    private val manager: NotificationReaderSyncManager,
    private val cacheRepo: CacheDataStoreRepository,
    private val settingsRepo: SettingsDataStoreRepository,
    private val readerRepo: NotificationReaderRepository,
    state: SavedStateHandle,
) : BaseHandleableViewModel(state) {

    private val _serviceTargetState by persistableLiveData<ServiceTargetState?>()

    val serviceTargetState = _serviceTargetState as LiveData<ServiceTargetState?>

    val notificationsItems = readerRepo.getNotifications().asLiveData().map { list ->
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

    var lastStartResult by persistableValueInitial<ManagerStartResult?>(null)
        private set

    var lastStopResult by persistableValueInitial<ManagerStopResult?>(null)
        private set

    override fun onInitialized() {
        super.onInitialized()
        if (_serviceTargetState.value == null) {
            cacheRepo.shouldNotificationReaderManagerRun.asLiveData().observeOnce(this) {
                _serviceTargetState.setValueIfNew(ServiceTargetState(it, false))
            }
        }
        serviceTargetState.observe {
            // костыль из-за Android Navigation с BottomNavigation:
            // экран пересоздаётся и используется initial вместо последнего значения,
            // + не вызывается onCleared -> нужно актуализировать всегда
            viewModelScope.launch {
                if (it != null) {
                    cacheRepo.setShouldNotificationReaderRun(it.state)
                }
            }
        }
    }

    /**
     * @param resultFunc передаётся состояние с флагом о том, выполняется ли сервис
     * + результаты запуска и/или остановки
     */
    fun doStartOrStop(
        fragment: BaseVmFragment<*>,
        navigateToSettingsForStop: Boolean,
        resultFunc: ((Triple<Boolean, ManagerStartResult?, ManagerStopResult?>) -> Unit)? = null,
    ) {
        // post_notifications не является обязательным для работы сервиса,
        // но спрашиваем (при включённой настройке) чтобы нотификации от двух сервисов были
        doOnBatteryOptimizationWithPostNotificationsAsk(fragment, cacheRepo, settingsRepo) {
            with(fragment.requireContext()) {
                val isStarted: Boolean
                val startResult: ManagerStartResult?
                val stopResult: ManagerStopResult?
                if (_serviceTargetState.value?.state == true) {
                    startResult = doStartWithHandleResult(this)
                    stopResult = null
                    isStarted = startResult.isSuccess
                } else {
                    stopResult = doStopWithHandleResult(this, navigateToSettingsForStop)
                    startResult = null
                    isStarted = !stopResult.isSuccess && stopResult != ManagerStopResult.SETTINGS_NEEDED
                }
                lastStartResult = startResult
                lastStopResult = stopResult
                resultFunc?.invoke(Triple(isStarted, startResult, stopResult))
            }
        }
    }

    fun onRemoveSuccessNotification(item: NotificationsAdapterData) {
        if (item.status !is NotificationReaderEntity.Success) return
        viewModelScope.launch {
            readerRepo.removeNotificationsByIds(listOf(item.id))
        }
    }

    fun onToggleServiceTargetStateAction() {
        _serviceTargetState.value = ServiceTargetState(!isServiceRunning(), true)
    }

    fun onDownloadPackageListAction() {
        if (!manager.isRunning.value) return
        if (!manager.doLaunchDownloadJobIfNeeded(
                    if (_serviceTargetState.value?.state == true) {
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
        // сервис может продолжать числитmся как выполняющийся
        // несмотря на убирание из настроек и stopService
        return NotificationReaderListenerService.isRunning() && manager.isRunning.value /*&& serviceTargetState.value != false*/
    }

    private fun doStartWithHandleResult(context: Context): ManagerStartResult {
        return manager.doStart(context).also {
            when (it) {
                ManagerStartResult.SERVICE_START_FAILED -> {
                    showSnackbar(TextMessage(R.string.notification_reader_snack_cannot_start_service))
                }

                ManagerStartResult.SETTINGS_NEEDED -> {
                    showToast(TextMessage(R.string.notification_reader_toast_start_add_in_settings))
                }

                else -> {
                }
            }
        }
    }

    /**
     * @param navigateToSettings false если вернулись с экрана настроек (или в onResume)
     */
    private fun doStopWithHandleResult(context: Context, navigateToSettings: Boolean): ManagerStopResult {
        // при возврате с экрана настроек, когда разрешение было отозвано,
        // можно попытаться остановить ещё раз
        return manager.doStop(context, navigateToSettings).also {
            if (it == ManagerStopResult.SETTINGS_NEEDED && navigateToSettings) {
                showToast(TextMessage(R.string.notification_reader_toast_stop_remove_in_settings))
            }
        }
    }

    data class ServiceTargetState(
        val state: Boolean,
        val changedFromView: Boolean,
    ) : Serializable
}