package net.maxsmr.feature.preferences.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.live.field.clearErrorOnChange
import net.maxsmr.commonutils.live.field.validateAndSetByRequired
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.delegates.persistableLiveData
import net.maxsmr.core.android.base.delegates.persistableValue
import net.maxsmr.core.android.network.URL_PAGE_BLANK
import net.maxsmr.core.ui.fields.LongFieldState
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.asYesNoNeutralDialog
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.core.ui.fields.toggleRequiredFieldState
import net.maxsmr.core.ui.fields.urlField
import net.maxsmr.feature.preferences.data.domain.AppSettings
import net.maxsmr.feature.preferences.data.domain.AppSettings.Companion.UPDATE_NOTIFICATION_INTERVAL_MIN
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsDataStoreRepository,
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

    val retryDownloadsField: Field<Boolean> = Field.Builder(false)
        .emptyIf { false }
        .persist(state, KEY_FIELD_RETRY_DOWNLOADS)
        .build()

    val disableNotificationsField: Field<Boolean> = Field.Builder(false)
        .emptyIf { false }
        .persist(state, KEY_FIELD_DISABLE_NOTIFICATIONS)
        .build()

    val updateNotificationIntervalStateField: Field<LongFieldState> = Field.Builder(LongFieldState(0))
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

    val startPageUrlField = state.urlField(
        R.string.settings_field_start_page_url_hint,
        isRequired = false,
        isValidByBlank = true
    )

    private val allFields =
        listOf<Field<*>>(
            maxDownloadsField,
            connectTimeoutField,
            retryDownloadsField,
            disableNotificationsField,
            updateNotificationIntervalStateField,
            startPageUrlField
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
                val settings = repository.getSettings()
                initialSettings = settings
                appSettings.value = settings
                restoreFields(settings)
            }
        }

        maxDownloadsField.clearErrorOnChange(this) {
            appSettings.value = currentAppSettings.copy(maxDownloads = it)
        }

        connectTimeoutField.clearErrorOnChange(this) {
            appSettings.value = currentAppSettings.copy(connectTimeout = it)
        }

        retryDownloadsField.valueLive.observe {
            appSettings.value = currentAppSettings.copy(retryDownloads = it)
        }

        disableNotificationsField.valueLive.observe {
            appSettings.value = currentAppSettings.copy(disableNotifications = it)
            updateNotificationIntervalStateField.toggleRequiredFieldState(
                !it,
                net.maxsmr.core.ui.R.string.field_error_empty,
                LongFieldState(0)
            )
        }

        updateNotificationIntervalStateField.clearErrorOnChange(this) {
            appSettings.value = currentAppSettings.copy(updateNotificationInterval = it.value)
        }
        startPageUrlField.clearErrorOnChange(this) {
            appSettings.value = currentAppSettings.copy(startPageUrl = it)
        }
    }

    override fun handleAlerts(context: Context, delegate: AlertFragmentDelegate<*>) {
        super.handleAlerts(context, delegate)
        delegate.bindAlertDialog(DIALOG_TAG_CONFIRM_EXIT) {
            it.asYesNoNeutralDialog(context)
        }
    }

    fun saveChanges() {
        if (!allFields.validateAndSetByRequired()) {
            return
        }
        viewModelScope.launch {
            if (hasChanges.value == true) {
                val initialSettings = initialSettings ?: AppSettings()
                repository.updateSettings(
                    AppSettings(
                        maxDownloadsField.value ?: initialSettings.maxDownloads,
                        connectTimeoutField.value ?: initialSettings.connectTimeout,
                        disableNotificationsField.value ?: initialSettings.disableNotifications,
                        retryDownloadsField.value ?: initialSettings.retryDownloads,
                        updateNotificationIntervalStateField.value?.value
                            ?: initialSettings.updateNotificationInterval,
                        startPageUrlField.value?.takeIf { it.isNotEmpty() } ?: URL_PAGE_BLANK
                    )
                )
            }
            navigateBack()
        }
    }

    fun navigateBackWithAlert() {
        if (hasChanges.value == true) {
            AlertBuilder(DIALOG_TAG_CONFIRM_EXIT)
                .setMessage(R.string.settings_alert_confirm_message)
                .setAnswers(
                    Alert.Answer(R.string.settings_alert_confirm_yes_button).onSelect {
                        saveChanges()
                    },
                    Alert.Answer(R.string.settings_alert_confirm_neutral_button).onSelect {
                        navigateBack()
                    },
                    Alert.Answer(R.string.settings_alert_confirm_negative_button),
                )
                .build()
        } else {
            navigateBack()
        }
    }

    private fun restoreFields(settings: AppSettings) {
        // используется для того, чтобы выставить initial'ы в филды
        maxDownloadsField.value = settings.maxDownloads
        connectTimeoutField.value = settings.connectTimeout
        retryDownloadsField.value = settings.retryDownloads
        disableNotificationsField.value = settings.disableNotifications
        updateNotificationIntervalStateField.value =
            LongFieldState(settings.updateNotificationInterval, !settings.disableNotifications)
        startPageUrlField.value = settings.startPageUrl
    }

    companion object {

        const val DIALOG_TAG_CONFIRM_EXIT = "confirm_exit"

        const val KEY_FIELD_MAX_DOWNLOADS = "max_downloads"
        const val KEY_FIELD_CONNECT_TIMEOUT = "connect_timeout"
        const val KEY_FIELD_RETRY_DOWNLOADS = "retry_downloads"
        const val KEY_FIELD_DISABLE_NOTIFICATIONS = "disable_notifications"
        const val KEY_FIELD_UPDATE_NOTIFICATION_INTERVAL_STATE = "update_notification_interval_state"
    }
}