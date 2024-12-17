package net.maxsmr.feature.notification_reader.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.format.formatDate
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.live.observeOnce
import net.maxsmr.commonutils.live.setValueIfNew
import net.maxsmr.commonutils.live.zip
import net.maxsmr.commonutils.states.ILoadState.Companion.copyOf
import net.maxsmr.commonutils.states.LoadState
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.android.base.delegates.persistableLiveData
import net.maxsmr.core.android.base.delegates.persistableLiveDataInitial
import net.maxsmr.core.android.base.delegates.persistableValueInitial
import net.maxsmr.core.database.model.notification_reader.NotificationReaderEntity
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.core.ui.components.fragments.BaseVmFragment
import net.maxsmr.feature.download.data.DownloadsViewModel
import net.maxsmr.feature.notification_reader.data.NotificationReaderListenerService
import net.maxsmr.feature.notification_reader.data.NotificationReaderRepository
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager.Companion.DOWNLOAD_TAG_PACKAGE_LIST
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager.ManagerStartResult
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager.ManagerStopResult
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager.StartMode
import net.maxsmr.feature.notification_reader.ui.adapter.NotificationsAdapterData
import net.maxsmr.feature.notification_reader.ui.adapter.NotificationsAdapterData.Companion.NOTIFICATION_DATETIME_FORMAT
import net.maxsmr.feature.notification_reader.ui.adapter.PackageNameAdapterData
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.feature.preferences.ui.doOnBatteryOptimizationWithPostNotificationsAsk
import java.io.Serializable
import java.util.Date

class NotificationReaderViewModel @AssistedInject constructor(
    @Assisted state: SavedStateHandle,
    @Assisted private val downloadsViewModel: DownloadsViewModel,
    private val manager: NotificationReaderSyncManager,
    private val cacheRepo: CacheDataStoreRepository,
    private val settingsRepo: SettingsDataStoreRepository,
    private val readerRepo: NotificationReaderRepository,
) : BaseHandleableViewModel(state) {

    private val _serviceTargetState by persistableLiveData<ServiceTargetState?>()

    val serviceTargetState = _serviceTargetState as LiveData<ServiceTargetState?>

    private val _packageListLoadState = MutableLiveData<LoadState<PackageListState>>(LoadState.success(null))

    val packageListLoadState = _packageListLoadState as LiveData<LoadState<PackageListState>>

    private val _packageListExpandedState by persistableLiveDataInitial(false)

    val packageListExpandedState = _packageListExpandedState as LiveData<Boolean>

    val settings = settingsRepo.settingsFlow.asLiveData()

    val isRunning = manager.isRunning.asLiveData()

    val notificationsItems = zip(readerRepo.getNotifications().asLiveData(), isRunning) { list, isRunning ->
        list to isRunning
    }.map { (list, isRunning) ->
        list.orEmpty().map {
            NotificationsAdapterData(
                it.id,
                it.contentText,
                it.packageName,
                formatDate(Date(it.timestamp), NOTIFICATION_DATETIME_FORMAT),
                it.status,
                isRunning ?: false
            )
        }
    }

    val inputApiKeyField = Field.Builder(EMPTY_STRING)
        .emptyIf { it.isEmpty() }
        .persist(state, KEY_FIELD_INPUT_API_KEY)
        .build()

    var lastStartResult by persistableValueInitial<ManagerStartResult?>(null)
        private set

    var lastStopResult by persistableValueInitial<ManagerStopResult?>(null)
        private set

    init {
        // обозревание на постоянной основе загрузки с тегом (целевой url может меняться);
        // при этом может появляться и пропадать в resultItems манагера
        // + настройка "белый список"
        zip(
            downloadsViewModel.observeDownload(DOWNLOAD_TAG_PACKAGE_LIST) {
                tag == it
            },
            cacheRepo.packageList.asLiveData(),
            settings
        ) { loadState, packageList, settings ->
            Triple(loadState, packageList, settings)
        }.observe {
            val (loadState, packageList, settings) = it
            viewModelScope.launch {
                _packageListLoadState.postValue(
                    if (loadState != null && !loadState.isSuccessWithData()) {
                        if (!settings?.packageListUrl.isNullOrEmpty()) {
                            loadState.copyOf(null)
                        } else {
                            LoadState.success(null)
                        }
                    } else {
                        LoadState.success(
                            PackageListState(
                                packageList.orEmpty().map { name -> PackageNameAdapterData(name) },
                                settings?.isWhitePackageList == true
                            )
                        )
                    }
                )
            }
        }
        viewModelScope.launch {
            _packageListLoadState.value = LoadState.success(
                PackageListState(
                    cacheRepo.getPackageList().map { name -> PackageNameAdapterData(name) },
                    settingsRepo.getSettings().isWhitePackageList
                )
            )
        }
    }

    override fun onInitialized() {
        super.onInitialized()
        if (_serviceTargetState.value == null) {
            cacheRepo.shouldNotificationReaderManagerRun.asLiveData().observeOnce(this) {
                _serviceTargetState.setValueIfNew(ServiceTargetState(it, false))
            }
        }
        serviceTargetState.observe {
            // актуализация целевого состояния в cacheRepo при каждом изменении
            viewModelScope.launch {
                if (it != null) {
                    cacheRepo.setShouldNotificationReaderRun(it.state)
                }
            }
        }
        settings.observeOnce {
            // при попадании на экран проверяем, был ли указан ключ
            if (it.notificationsApiKey.isEmpty()) {
                showInputApiKeyDialog()
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
        val context = fragment.requireContext()
        if (serviceTargetState.value?.state == true) {
            // post_notifications не является обязательным для работы сервиса,
            // но спрашиваем (при включённой настройке) чтобы нотификации от двух сервисов были
            doOnBatteryOptimizationWithPostNotificationsAsk(fragment, cacheRepo, settingsRepo) {
                val startResult = doStartWithHandleResult(context)
                val isStarted = startResult.isSuccess
                lastStartResult = startResult
                lastStopResult = null
                resultFunc?.invoke(Triple(isStarted, startResult, null))
            }
        } else {
            val stopResult: ManagerStopResult = doStopWithHandleResult(context, navigateToSettingsForStop)
            val isStarted = !stopResult.isSuccess && stopResult != ManagerStopResult.SETTINGS_NEEDED
            lastStopResult = stopResult
            lastStartResult = null
            resultFunc?.invoke(Triple(isStarted, null, stopResult))
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

    fun onClearSuccessAction() {
        viewModelScope.launch {
            readerRepo.removeNotifications(
                readerRepo.getNotificationsRaw { status is NotificationReaderEntity.Success }
            )
        }
    }

    fun onRetryFailedNotificationsAction() {
        if (!manager.isRunning.value) {
            showToast(TextMessage(R.string.notification_reader_toast_service_not_running))
            return
        }
        manager.retryFailedNotifications()
    }

    fun onRetryFailedNotification(id: Long) {
        if (!manager.isRunning.value) {
            showToast(TextMessage(R.string.notification_reader_toast_service_not_running))
            return
        }
        manager.retryFailedNotification(id)
    }

    fun onTogglePackageListExpandedState() {
        _packageListExpandedState.value = _packageListExpandedState.value == false
    }

    fun resetServiceTargetStateViewFlag() {
        _serviceTargetState.setValueIfNew(_serviceTargetState.value?.copy(changedFromView = false))
    }

    fun showInputApiKeyDialog() {
        viewModelScope.launch {
            inputApiKeyField.value = settingsRepo.getSettings().notificationsApiKey
            showOkDialog(
                DIALOG_TAG_INPUT_API_KEY,
                message = TextMessage(R.string.notification_reader_dialog_input_api_key_message),
            )
        }
    }

    fun onInputApiKeyDialogConfirm() {
        viewModelScope.launch {
            val value = inputApiKeyField.value
            val settings = settingsRepo.getSettings()
            val previousValue = settings.notificationsApiKey
            if (previousValue != value) {
                settingsRepo.updateSettings(
                    settings.copy(notificationsApiKey = value)
                )
                if (value.isNotEmpty() && previousValue.isEmpty() && serviceTargetState.value?.state != true) {
                    // ключ был указан -> целевое состояние меняется на запущенное
                    _serviceTargetState.postValue(ServiceTargetState(state = true, changedFromView = true))
                }
            }
        }
    }

    fun isServiceRunning(): Boolean {
        // сервис может продолжать числитmся как выполняющийся
        // несмотря на убирание из настроек и stopService
        return NotificationReaderListenerService.isRunning() && manager.isRunning.value /*&& serviceTargetState.value != false*/
    }

    private fun doStartWithHandleResult(context: Context): ManagerStartResult {
        return manager.doStart(context).also {
            logger.d("doStart result: $it")
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
            logger.d("doStop result: $it")
            if (it == ManagerStopResult.SETTINGS_NEEDED && navigateToSettings) {
                showToast(TextMessage(R.string.notification_reader_toast_stop_remove_in_settings))
            }
        }
    }

    data class ServiceTargetState(
        val state: Boolean,
        val changedFromView: Boolean,
    ) : Serializable

    data class PackageListState(
        val names: List<PackageNameAdapterData>,
        val isWhiteList: Boolean,
    )

    @AssistedFactory
    interface Factory {

        fun create(
            state: SavedStateHandle,
            downloadsViewModel: DownloadsViewModel,
        ): NotificationReaderViewModel
    }

    companion object {

        const val DIALOG_TAG_INPUT_API_KEY = "input_api_key"
        const val KEY_FIELD_INPUT_API_KEY = "input_api_key"
    }
}