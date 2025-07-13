package net.maxsmr.feature.preferences.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.flow.field.Field
import net.maxsmr.commonutils.flow.field.observeWithClearError
import net.maxsmr.commonutils.flow.field.validateAndSetByRequiredFields
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.isAtLeastTiramisu
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.delegates.persistableValueInitial
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.RoutingApp
import net.maxsmr.core.domain.entities.feature.settings.AppSettings
import net.maxsmr.core.domain.entities.feature.settings.AppSettings.Companion.UPDATE_NOTIFICATION_INTERVAL_MIN
import net.maxsmr.core.ui.field.BooleanFieldWithState
import net.maxsmr.core.ui.field.LongFieldWithState
import net.maxsmr.core.ui.field.createNonEmptyField
import net.maxsmr.core.ui.field.toggleRequiredFieldState
import net.maxsmr.core.ui.field.urlField
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsDataStoreRepository,
    val cacheRepository: CacheDataStoreRepository,
    state: SavedStateHandle,
) : BaseViewModel(state) {

    val maxDownloadsField: Field<Int> = createNonEmptyField(
        initialValue = 0,
        key = KEY_FIELD_MAX_DOWNLOADS
    ) {
        validators(Field.Validator(net.maxsmr.core.ui.R.string.field_error_value_negative) {
            it >= 0
        })
        hint(R.string.settings_field_max_downloads_hint)
    }

    val connectTimeoutField: Field<Long> = createNonEmptyField(
        initialValue = 0L,
        key = KEY_FIELD_CONNECT_TIMEOUT
    ) {
        validators(Field.Validator({
            return@Validator TextMessage(net.maxsmr.core.ui.R.string.field_error_value_negative)
        }) {
            it >= 0
        })
        hint(R.string.settings_field_connect_timeout_hint)
    }

    val loadByWiFiOnlyField: Field<Boolean> = createNonEmptyField(
        initialValue = false,
        key = KEY_FIELD_LOAD_BY_WI_FI_ONLY
    )

    val retryOnConnectionFailureField: Field<Boolean> = createNonEmptyField(
        initialValue = false,
        key = KEY_FIELD_RETRY_ON_CONNECTION_FAILURE
    )

    val retryDownloadsField: Field<Boolean> = createNonEmptyField(
        initialValue = false,
        key = KEY_FIELD_RETRY_DOWNLOADS
    )

    val disableNotificationsField: Field<Boolean> = createNonEmptyField(
        initialValue = false,
        key = KEY_FIELD_DISABLE_NOTIFICATIONS
    )

    val updateNotificationIntervalStateField: Field<LongFieldWithState> = createNonEmptyField(
        initialValue = LongFieldWithState(0),
        key = KEY_FIELD_UPDATE_NOTIFICATION_INTERVAL_STATE
    ) {
        validators(Field.Validator({
            return@Validator TextMessage(
                net.maxsmr.core.ui.R.string.field_error_value_more_or_equal_format,
                UPDATE_NOTIFICATION_INTERVAL_MIN
            )
        }) {
            it.value >= UPDATE_NOTIFICATION_INTERVAL_MIN
        })
        hint(R.string.settings_field_update_notification_interval_hint)
    }

    val openLinksInExternalAppsField: Field<BooleanFieldWithState> = createNonEmptyField(
        initialValue = BooleanFieldWithState(false),
        key = KEY_FIELD_OPEN_LINKS_IN_EXTERNAL_APPS
    )

    val startPageUrlField = urlField(
        hintResId = R.string.settings_field_start_page_url_hint,
        isRequired = false,
        isValidByBlank = true
    )

    val routingAppField: Field<RoutingApp> = createNonEmptyField(
        initialValue = RoutingApp.DOUBLEGIS,
        key = KEY_FIELD_ROUTING_APP
    )

    val routingAppFromCurrentField: Field<Boolean> = createNonEmptyField(
        initialValue = false,
        key = KEY_FIELD_ROUTING_APP_FROM_CURRENT
    )

    val hasChanges: StateFlow<Boolean> by lazy {
        _appSettings.map {
            it != initialSettings
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    }

    private val allFields = listOf<Field<*>>(
        maxDownloadsField,
        connectTimeoutField,
        loadByWiFiOnlyField,
        retryOnConnectionFailureField,
        retryDownloadsField,
        disableNotificationsField,
        updateNotificationIntervalStateField,
        openLinksInExternalAppsField,
        startPageUrlField,
        routingAppField,
        routingAppFromCurrentField
    )

    private val _appSettings =
        MutableSharedFlow<AppSettings?>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val appSettings: StateFlow<AppSettings?> =
        _appSettings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val currentAppSettings: AppSettings get() = appSettings.value ?: AppSettings()

    private var initialSettings by persistableValueInitial<AppSettings?>(null)

    override fun onInitialized() {
        if (initialSettings == null) {
            viewModelScope.launch {
                updateSettings()
            }
        }

        maxDownloadsField.observeWithClearError(viewModelScope) {
            _appSettings.tryEmit(currentAppSettings.copy(maxDownloads = it))
        }

        connectTimeoutField.observeWithClearError(viewModelScope) {
            _appSettings.tryEmit(currentAppSettings.copy(connectTimeout = it))
        }

        loadByWiFiOnlyField.valueFlow.observe {
            _appSettings.tryEmit(currentAppSettings.copy(loadByWiFiOnly = it))
        }

        retryOnConnectionFailureField.valueFlow.observe {
            _appSettings.tryEmit(currentAppSettings.copy(retryOnConnectionFailure = it))
        }
        retryDownloadsField.valueFlow.observe {
            _appSettings.tryEmit(currentAppSettings.copy(retryDownloads = it))
        }

        disableNotificationsField.valueFlow.observe {
            _appSettings.tryEmit(currentAppSettings.copy(disableNotifications = it))
            updateNotificationIntervalStateField.toggleRequiredFieldState(
                !it,
                net.maxsmr.core.ui.R.string.field_error_empty,
            )
        }

        updateNotificationIntervalStateField.observeWithClearError(viewModelScope) {
            _appSettings.tryEmit(currentAppSettings.copy(updateNotificationInterval = it.value))
        }

        openLinksInExternalAppsField.valueFlow.observe {
            _appSettings.tryEmit(currentAppSettings.copy(openLinksInExternalApps = it.value))
        }

        startPageUrlField.observeWithClearError(viewModelScope) {
            _appSettings.tryEmit(currentAppSettings.copy(startPageUrl = it))
        }

        routingAppField.valueFlow.observe {
            _appSettings.tryEmit(currentAppSettings.copy(routingApp = it))
        }

        routingAppFromCurrentField.valueFlow.observe {
            _appSettings.tryEmit(currentAppSettings.copy(routingAppFromCurrent = it))
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
            if (!hasChanges.value) {
                return@launch
            }
            val disableNotifications = disableNotificationsField.value
            if (!disableNotifications) {
                viewModelScope.launch {
                    cacheRepository.clearPostNotificationAsked()
                }
            }

            repository.updateSettings(currentAppSettings)
            updateSettings()
            navigationAction?.invoke()
        }
    }

    fun navigateBackWithAlert(errorFieldResult: (Field<*>) -> Unit?): Boolean =
        navigateWithAlert(errorFieldResult) { navigateBack() }

    /**
     * @return true если навигация возможна, изменений нет; false - в ином случае
     */
    fun navigateWithAlert(
        errorFieldResult: (Field<*>) -> Unit?,
        navigationAction: (() -> Unit)?,
    ): Boolean {
        return if (hasChanges.value) {
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
            false
        } else {
            true
        }
    }

    private fun restoreFields(settings: AppSettings) {
        // используется для того, чтобы выставить initial'ы в филды
        maxDownloadsField.value = settings.maxDownloads
        connectTimeoutField.value = settings.connectTimeout
        loadByWiFiOnlyField.value = settings.loadByWiFiOnly
        retryOnConnectionFailureField.value = settings.retryOnConnectionFailure
        retryDownloadsField.value = settings.retryDownloads
        disableNotificationsField.value = settings.disableNotifications
        updateNotificationIntervalStateField.value =
            LongFieldWithState(settings.updateNotificationInterval, !settings.disableNotifications)
        openLinksInExternalAppsField.value =
            BooleanFieldWithState(settings.openLinksInExternalApps, isAtLeastTiramisu())
        startPageUrlField.value = settings.startPageUrl
        routingAppField.value = settings.routingApp
        routingAppFromCurrentField.value = settings.routingAppFromCurrent
    }

    private suspend fun updateSettings() {
        val settings = repository.getSettings()
        initialSettings = settings
        _appSettings.tryEmit(settings)
        restoreFields(settings)
    }

    companion object {

        const val DIALOG_TAG_CONFIRM_EXIT = "confirm_exit"

        const val KEY_FIELD_MAX_DOWNLOADS = "max_downloads"
        const val KEY_FIELD_CONNECT_TIMEOUT = "connect_timeout"
        const val KEY_FIELD_LOAD_BY_WI_FI_ONLY = "load_by_wi_fi_only"
        const val KEY_FIELD_RETRY_ON_CONNECTION_FAILURE = "retry_on_connection_failure"
        const val KEY_FIELD_RETRY_DOWNLOADS = "retry_downloads"
        const val KEY_FIELD_DISABLE_NOTIFICATIONS = "disable_notifications"
        const val KEY_FIELD_UPDATE_NOTIFICATION_INTERVAL_STATE = "update_notification_interval_state"
        const val KEY_FIELD_OPEN_LINKS_IN_EXTERNAL_APPS = "open_links_in_external_apps"
        const val KEY_FIELD_ROUTING_APP = "routing_app"
        const val KEY_FIELD_ROUTING_APP_FROM_CURRENT = "routing_app_from_current"
    }
}