package net.maxsmr.feature.preferences.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.live.field.clearErrorOnChange
import net.maxsmr.commonutils.live.field.validateAndSetByRequired
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.delegates.persistableLiveData
import net.maxsmr.core.android.base.delegates.persistableValue
import net.maxsmr.core.ui.LongFieldState
import net.maxsmr.core.ui.toggleRequiredFieldState
import net.maxsmr.feature.preferences.data.domain.AppSettings
import net.maxsmr.feature.preferences.data.domain.AppSettings.Companion.UPDATE_NOTIFICATION_INTERVAL_MIN
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsDataStoreRepository,
    state: SavedStateHandle,
) : BaseViewModel(state) {

    val maxDownloadsField: Field<Int> = Field.Builder(0)
        .emptyIf { false }
        .setRequired(R.string.settings_field_empty_error)
        .validators(Field.Validator(R.string.settings_field_max_downloads_error) {
            it > 0
        })
        .hint(R.string.settings_field_max_downloads_hint)
        .persist(state, KEY_FIELD_MAX_DOWNLOADS)
        .build()

    val disableNotificationsField: Field<Boolean> = Field.Builder(false)
        .emptyIf { false }
        .setRequired(R.string.settings_field_empty_error)
        .persist(state, KEY_FIELD_DISABLE_NOTIFICATIONS)
        .build()

    val updateNotificationIntervalStateField: Field<LongFieldState> = Field.Builder(LongFieldState(0))
        .emptyIf { false }
        .setRequired(R.string.settings_field_empty_error)
        .validators(Field.Validator({
            return@Validator TextMessage(R.string.settings_field_update_notification_interval_error_format, UPDATE_NOTIFICATION_INTERVAL_MIN)
        }) {
            it.value >= UPDATE_NOTIFICATION_INTERVAL_MIN
        })
        .hint(R.string.settings_field_update_notification_interval_hint)
        .persist(state, KEY_FIELD_UPDATE_NOTIFICATION_INTERVAL_STATE)
        .build()

    private val allFields =
        listOf<Field<*>>(maxDownloadsField, disableNotificationsField, updateNotificationIntervalStateField)

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
                val settings = repository.settings.firstOrNull() ?: AppSettings()
                initialSettings = settings
                appSettings.value = settings
                restoreFields(settings)
            }
        }

        maxDownloadsField.clearErrorOnChange(this) {
            appSettings.value = currentAppSettings.copy(maxDownloads = it)
        }

        disableNotificationsField.valueLive.observe {
            appSettings.value = currentAppSettings.copy(disableNotifications = it)
            updateNotificationIntervalStateField.toggleRequiredFieldState(
                !it,
                R.string.settings_field_empty_error,
                LongFieldState(0)
            )
        }

        updateNotificationIntervalStateField.clearErrorOnChange(this) {
            appSettings.value = currentAppSettings.copy(updateNotificationInterval = it.value)
        }
    }

    fun saveChanges() {

        fun <D> Field<D>.getIfRequired(): D? {
            return if (required) {
                value
            } else {
                null
            }
        }

        if (!allFields.validateAndSetByRequired(false)) {
            return
        }
        viewModelScope.launch {
            if (hasChanges.value == true) {
                val initialSettings = initialSettings ?: AppSettings()
                repository.updateSettings(
                    AppSettings(
                        maxDownloadsField.getIfRequired() ?: initialSettings.maxDownloads,
                        disableNotificationsField.getIfRequired() ?: initialSettings.disableNotifications,
                        updateNotificationIntervalStateField.getIfRequired()?.value
                            ?: initialSettings.updateNotificationInterval
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
        disableNotificationsField.value = settings.disableNotifications
        updateNotificationIntervalStateField.value =
            LongFieldState(settings.updateNotificationInterval, !settings.disableNotifications)
    }

    companion object {

        const val DIALOG_TAG_CONFIRM_EXIT = "confirm_exit"

        const val KEY_FIELD_MAX_DOWNLOADS = "max_downloads"
        const val KEY_FIELD_DISABLE_NOTIFICATIONS = "disable_notifications"
        const val KEY_FIELD_UPDATE_NOTIFICATION_INTERVAL_STATE = "update_notification_interval_state"
    }
}