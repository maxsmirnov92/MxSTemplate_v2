package net.maxsmr.feature.preferences.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.isAtLeastTiramisu
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.live.field.clearErrorOnChange
import net.maxsmr.commonutils.live.field.validateAndSetByRequiredFields
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.delegates.persistableLiveData
import net.maxsmr.core.android.base.delegates.persistableValue
import net.maxsmr.core.android.network.URL_PAGE_BLANK
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.asYesNoNeutralDialog
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.core.ui.fields.BooleanFieldWithState
import net.maxsmr.core.ui.fields.LongFieldWithState
import net.maxsmr.core.ui.fields.toggleRequiredFieldState
import net.maxsmr.core.ui.fields.urlField
import net.maxsmr.feature.preferences.data.domain.AppSettings
import net.maxsmr.feature.preferences.data.domain.AppSettings.Companion.UPDATE_NOTIFICATION_INTERVAL_MIN
import net.maxsmr.feature.preferences.data.repository.CacheDataStoreRepository
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.RoutingApp

import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsDataStoreRepository,
    val cacheRepository: CacheDataStoreRepository,
    state: SavedStateHandle,
) : BaseHandleableViewModel(state) {

    val maxDownloadsField: Field<Int> = Field.Builder(0)
        .emptyIf { false }
        .validators(Field.Validator(net.maxsmr.core.ui.R.string.field_error_value_negative) {
            it >= 0
        })
        .hint(R.string.settings_field_max_downloads_hint)
        .persist(state, KEY_FIELD_MAX_DOWNLOADS)
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

    val updateNotificationIntervalStateField: Field<LongFieldWithState> = Field.Builder(LongFieldWithState(0))
        .emptyIf { false }
        .validators(Field.Validator({
            return@Validator TextMessage(
                net.maxsmr.core.ui.R.string.field_error_value_more_or_equal_format,
                UPDATE_NOTIFICATION_INTERVAL_MIN
            )
        }) {
            it.value >= UPDATE_NOTIFICATION_INTERVAL_MIN
        })
        .hint(R.string.settings_field_update_notification_interval_hint)
        .persist(state, KEY_FIELD_UPDATE_NOTIFICATION_INTERVAL_STATE)
        .build()

    val openLinksInExternalAppsField: Field<BooleanFieldWithState> = Field.Builder(BooleanFieldWithState(false))
        .emptyIf { false }
        .persist(state, KEY_FIELD_OPEN_LINKS_IN_EXTERNAL_APPS)
        .build()

    val startPageUrlField = state.urlField(
        R.string.settings_field_start_page_url_hint,
        isRequired = false,
        isValidByBlank = true
    )

    val routingAppField: Field<RoutingApp> = Field.Builder(RoutingApp.DOUBLEGIS)
        .emptyIf { false }
        .persist(state, KEY_FIELD_ROUTING_APP)
        .build()

    val routingAppFromCurrentField: Field<Boolean> = Field.Builder(false)
        .emptyIf { false }
        .persist(state, KEY_FIELD_ROUTING_APP_FROM_CURRENT)
        .build()

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

        maxDownloadsField.clearErrorOnChange(this) {
            appSettings.value = currentAppSettings.copy(maxDownloads = it)
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
            updateNotificationIntervalStateField.toggleRequiredFieldState(
                !it,
                net.maxsmr.core.ui.R.string.field_error_empty,
            )
        }

        updateNotificationIntervalStateField.clearErrorOnChange(this) {
            appSettings.value = currentAppSettings.copy(updateNotificationInterval = it.value)
        }

        openLinksInExternalAppsField.valueLive.observe {
            appSettings.value = currentAppSettings.copy(openLinksInExternalApps = it.value)
        }

        startPageUrlField.clearErrorOnChange(this) {
            appSettings.value = currentAppSettings.copy(startPageUrl = it)
        }

        routingAppField.valueLive.observe {
            appSettings.value = currentAppSettings.copy(routingApp = it)
        }

        routingAppFromCurrentField.valueLive.observe {
            appSettings.value = currentAppSettings.copy(routingAppFromCurrent = it)
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
        openLinksInExternalAppsField.value = BooleanFieldWithState(settings.openLinksInExternalApps, isAtLeastTiramisu())
        startPageUrlField.value = settings.startPageUrl
        routingAppField.value = settings.routingApp
        routingAppFromCurrentField.value = settings.routingAppFromCurrent
    }

    private suspend fun updateSettings() {
        val settings = repository.getSettings()
        initialSettings = settings
        appSettings.value = settings
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