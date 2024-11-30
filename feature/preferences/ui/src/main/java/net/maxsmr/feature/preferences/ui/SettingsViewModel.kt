package net.maxsmr.feature.preferences.ui

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.isAtLeastOreo
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.live.field.clearErrorOnChange
import net.maxsmr.commonutils.live.field.validateAndSetByRequiredFields
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.delegates.persistableLiveData
import net.maxsmr.core.android.base.delegates.persistableValue
import net.maxsmr.core.android.coroutines.usecase.UseCaseResult
import net.maxsmr.core.domain.entities.feature.settings.AppSettings
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.asYesNoNeutralDialog
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.core.ui.fields.urlField
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsDataStoreRepository,
    private val keyImportUseCase: NotificationReaderKeyImportUseCase,
    private val cacheRepository: CacheDataStoreRepository,
    state: SavedStateHandle,
) : BaseHandleableViewModel(state) {

    val notificationsUrlField = state.urlField(
        R.string.settings_field_notifications_url_hint,
        isRequired = true,
        isValidByBlank = false,
        key = KEY_FIELD_URL_NOTIFICATIONS,
    )

    val packageListUrlField = state.urlField(
        R.string.settings_field_package_list_url_hint,
        isRequired = true,
        isValidByBlank = false,
        key = KEY_FIELD_URL_PACKAGE_LIST
    )

    val isWhitePackageListField: Field<Boolean> = Field.Builder(false)
        .emptyIf { false }
        .persist(state, KEY_FIELD_IS_WHITE_PACKAGE_LIST)
        .build()

    val failedNotificationsWatcherIntervalField: Field<Long> = Field.Builder(0L)
        .emptyIf { false }
        .validators(Field.Validator({
            return@Validator TextMessage(net.maxsmr.core.ui.R.string.field_error_value_negative)
        }) {
            it >= 0
        })
        .hint(R.string.settings_field_failed_notifications_watcher_interval_hint)
        .persist(state, KEY_FIELD_FAILED_NOTIFICATIONS_WATCHER_INTERVAL)
        .build()

    val successNotificationsLifeTimeField: Field<Long> = Field.Builder(0L)
        .emptyIf { false }
        .validators(Field.Validator({
            return@Validator TextMessage(net.maxsmr.core.ui.R.string.field_error_value_negative)
        }) {
            it >= 0
        })
        .hint(R.string.settings_field_success_notifications_life_time_hint)
        .persist(state, KEY_FIELD_SUCCESS_NOTIFICATIONS_LIFE_TIME)
        .build()

    val connectTimeoutField: Field<Long> = Field.Builder(0L)
        .emptyIf { false }
        .validators(Field.Validator({
            return@Validator TextMessage(net.maxsmr.core.ui.R.string.field_error_value_negative)
        }) {
            it >= 0
        })
        .hint(R.string.settings_field_connect_timeout_hint)
        .persist(state, KEY_FIELD_CONNECT_TIMEOUT)
        .build()

    val loadByWiFiOnlyField: Field<Boolean> = Field.Builder(false)
        .emptyIf { false }
        .persist(state, KEY_FIELD_LOAD_BY_WI_FI_ONLY)
        .build()

    val retryOnConnectionFailureField: Field<Boolean> = Field.Builder(false)
        .emptyIf { false }
        .persist(state, KEY_FIELD_RETRY_ON_CONNECTION_FAILURE)
        .build()

    val retryDownloadsField: Field<Boolean> = Field.Builder(false)
        .emptyIf { false }
        .persist(state, KEY_FIELD_RETRY_DOWNLOADS)
        .build()

    val disableNotificationsField: Field<Boolean> = Field.Builder(false)
        .emptyIf { false }
        .persist(state, KEY_FIELD_DISABLE_NOTIFICATIONS)
        .build()

    val canDrawOverlaysField: Field<Boolean>? = if (isAtLeastOreo()) {
        Field.Builder(false)
            .emptyIf { false }
            .persist(state, KEY_FIELD_CAN_DRAW_OVERLAYS)
            .build()
    } else {
        null
    }

    private val allFields = mutableListOf<Field<*>>(
        notificationsUrlField,
        packageListUrlField,
        isWhitePackageListField,
        failedNotificationsWatcherIntervalField,
        successNotificationsLifeTimeField,
        connectTimeoutField,
        loadByWiFiOnlyField,
        retryOnConnectionFailureField,
        retryDownloadsField,
        disableNotificationsField,
    ).apply {
        canDrawOverlaysField?.let {
            add(it)
        }
    }

    private val appSettings by persistableLiveData<AppSettings>()

    private val currentAppSettings: AppSettings get() = appSettings.value ?: AppSettings()

    val hasChanges = appSettings.map {
        it != initialSettings
    }

    private var initialSettings by persistableValue<AppSettings>()

    override fun onInitialized() {
        super.onInitialized()
        if (initialSettings == null) {
            viewModelScope.launch {
                updateSettings()
            }
        }

        notificationsUrlField.clearErrorOnChange(this) {
            appSettings.value = currentAppSettings.copy(notificationsUrl = it)
        }

        packageListUrlField.clearErrorOnChange(this) {
            appSettings.value = currentAppSettings.copy(packageListUrl = it)
        }

        isWhitePackageListField.valueLive.observe {
            appSettings.value = currentAppSettings.copy(isWhitePackageList = it)
        }

        failedNotificationsWatcherIntervalField.clearErrorOnChange(this) {
            appSettings.value = currentAppSettings.copy(failedNotificationsWatcherInterval = it)
        }

        successNotificationsLifeTimeField.clearErrorOnChange(this) {
            appSettings.value = currentAppSettings.copy(successNotificationsLifeTime = it)
        }

        connectTimeoutField.clearErrorOnChange(this) {
            appSettings.value = currentAppSettings.copy(connectTimeout = it)
        }

        loadByWiFiOnlyField.valueLive.observe {
            appSettings.value = currentAppSettings.copy(loadByWiFiOnly = it)
        }

        retryOnConnectionFailureField.valueLive.observe {
            appSettings.value = currentAppSettings.copy(retryOnConnectionFailure = it)
        }

        retryDownloadsField.valueLive.observe {
            appSettings.value = currentAppSettings.copy(retryDownloads = it)
        }

        disableNotificationsField.valueLive.observe {
            appSettings.value = currentAppSettings.copy(disableNotifications = it)
        }

        canDrawOverlaysField?.valueLive?.observe {
            appSettings.value = currentAppSettings.copy(canDrawOverlays = it)
        }
    }

    override fun handleAlerts(delegate: AlertFragmentDelegate<*>) {
        super.handleAlerts(delegate)
        delegate.bindAlertDialog(DIALOG_TAG_CONFIRM_EXIT) {
            it.asYesNoNeutralDialog(delegate.context)
        }
    }

    fun saveChanges(
        errorFieldResult: (Field<*>) -> Unit?,
        navigationAction: (() -> Unit)? = null,
    ) {
        viewModelScope.launch {
            val result = allFields.validateAndSetByRequiredFields()
            if (result.isNotEmpty()) {
                errorFieldResult(result.first())
                return@launch
            }
            if (hasChanges.value != true) {
                return@launch
            }
            if (!disableNotificationsField.value) {
                viewModelScope.launch {
                    cacheRepository.clearPostNotificationAsked()
                }
            }
            canDrawOverlaysField?.let {
                if (it.value) {
                    viewModelScope.launch {
                        cacheRepository.clearCanDrawOverlaysAsked()
                    }
                }
            }

            repository.updateSettings(currentAppSettings)
            updateSettings()
            navigationAction?.invoke()
        }
    }

    fun navigateBackWithAlert(errorFieldResult: (Field<*>) -> Unit?): Boolean =
        navigateWithAlert(errorFieldResult) { navigateBack() }

    fun navigateWithAlert(
        errorFieldResult: (Field<*>) -> Unit?,
        navigationAction: (() -> Unit)?,
    ): Boolean {
        return if (hasChanges.value == true) {
            AlertDialogBuilder(DIALOG_TAG_CONFIRM_EXIT)
                .setMessage(R.string.settings_dialog_confirm_message)
                .setAnswers(
                    Alert.Answer(R.string.settings_dialog_confirm_yes_button).onSelect {
                        saveChanges(errorFieldResult, navigationAction)
                    },
                    Alert.Answer(R.string.settings_dialog_confirm_neutral_button).onSelect {
//                        navigateBack()
                        navigationAction?.invoke()
                    },
                    Alert.Answer(R.string.settings_dialog_confirm_negative_button),
                )
                .build()
            true
        } else {
            false
        }
    }

    fun onPickApiKeyFromFile(uri: Uri) {
        dialogQueue.toggle(true, DIALOG_TAG_PROGRESS)
        viewModelScope.launch {
            val result = keyImportUseCase.invoke(uri)
            dialogQueue.toggle(false, DIALOG_TAG_PROGRESS)
            if (result is UseCaseResult.Error) {
                showOkDialog(
                    DIALOG_TAG_IMPORT_FAILED,
                    result.errorMessage()?.let {
                        TextMessage(R.string.settings_dialog_key_import_error_message_format, it)
                    } ?: TextMessage(R.string.settings_dialog_key_import_error_message)
                )
            }
        }
    }

    private fun restoreFields(settings: AppSettings) {
        // используется для того, чтобы выставить initial'ы в филды
        notificationsUrlField.value = settings.notificationsUrl
        packageListUrlField.value = settings.packageListUrl
        isWhitePackageListField.value = settings.isWhitePackageList
        failedNotificationsWatcherIntervalField.value = settings.failedNotificationsWatcherInterval
        successNotificationsLifeTimeField.value = settings.successNotificationsLifeTime
        connectTimeoutField.value = settings.connectTimeout
        loadByWiFiOnlyField.value = settings.loadByWiFiOnly
        retryOnConnectionFailureField.value = settings.retryOnConnectionFailure
        retryDownloadsField.value = settings.retryDownloads
        disableNotificationsField.value = settings.disableNotifications
        canDrawOverlaysField?.value = settings.canDrawOverlays
    }

    private suspend fun updateSettings() {
        val settings = repository.getSettings()
        initialSettings = settings
        appSettings.value = settings
        restoreFields(settings)
    }

    companion object {

        const val DIALOG_TAG_CONFIRM_EXIT = "confirm_exit"
        const val DIALOG_TAG_IMPORT_FAILED = "import_failed"

        private const val KEY_FIELD_URL_NOTIFICATIONS = "url_notifications"
        private const val KEY_FIELD_URL_PACKAGE_LIST = "url_package_list"
        private const val KEY_FIELD_IS_WHITE_PACKAGE_LIST = "is_white_package_list"
        private const val KEY_FIELD_FAILED_NOTIFICATIONS_WATCHER_INTERVAL = "failed_notifications_watcher_interval"
        private const val KEY_FIELD_SUCCESS_NOTIFICATIONS_LIFE_TIME = "success_notifications_life_time"
        private const val KEY_FIELD_CONNECT_TIMEOUT = "connect_timeout"
        private const val KEY_FIELD_LOAD_BY_WI_FI_ONLY = "load_by_wi_fi_only"
        private const val KEY_FIELD_RETRY_ON_CONNECTION_FAILURE = "retry_on_connection_failure"
        private const val KEY_FIELD_RETRY_DOWNLOADS = "retry_downloads"
        private const val KEY_FIELD_DISABLE_NOTIFICATIONS = "disable_notifications"
        private const val KEY_FIELD_CAN_DRAW_OVERLAYS = "can_draw_overlays"
    }
}