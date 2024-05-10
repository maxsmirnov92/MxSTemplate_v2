package net.maxsmr.feature.preferences.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.live.observeOnce
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.delegates.persistableLiveData
import net.maxsmr.core.android.base.delegates.persistableValue
import net.maxsmr.feature.preferences.data.domain.AppSettings
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsDataStoreRepository,
    state: SavedStateHandle
): BaseViewModel(state) {

    val maxDownloadsField: Field<Int> = Field.Builder(0)
        .emptyIf { false }
        .hint(R.string.settings_field_hint_max_downloads)
        .persist(state, KEY_FIELD_MAX_DOWNLOADS)
        .build()

    val ignoreServerErrorField: Field<Boolean> = Field.Builder(false)
        .emptyIf { false }
        .persist(state, KEY_FIELD_IGNORE_SERVER_ERROR)
        .build()

    val deleteUnfinishedField: Field<Boolean> = Field.Builder(false)
        .emptyIf { false }
        .persist(state, KEY_FIELD_DELETE_UNFINISHED)
        .build()

    val disableNotificationsField: Field<Boolean> = Field.Builder(false)
        .emptyIf { false }
        .persist(state, KEY_FIELD_DISABLE_NOTIFICATIONS)
        .build()

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
        maxDownloadsField.valueLive.observe {
            appSettings.value = currentAppSettings.copy(maxDownloads = it)
        }
        ignoreServerErrorField.valueLive.observe {
            appSettings.value = currentAppSettings.copy(ignoreServerError = it)
        }
        deleteUnfinishedField.valueLive.observe {
            appSettings.value = currentAppSettings.copy(deleteUnfinished = it)
        }
        disableNotificationsField.valueLive.observe {
            appSettings.value = currentAppSettings.copy(disableNotifications = it)
        }
    }

    fun saveChanges() {
        viewModelScope.launch {
            if (hasChanges.value == true) {
                repository.updateSettings(currentAppSettings)
            }
            navigateBack()
        }
    }

    fun navigateBackWithAlert() {
        if (hasChanges.value == true) {
            AlertBuilder(DIALOG_TAG_CONFIRM)
                .setMessage(R.string.settings_dialog_confirm_message)
                .setAnswers(
                    Alert.Answer(R.string.settings_dialog_confirm_yes_button).onSelect {
                        saveChanges()
                    },
                    Alert.Answer(R.string.settings_dialog_confirm_neutral_button).onSelect {
                        navigateBack()
                    },
                    Alert.Answer(R.string.settings_dialog_confirm_negative_button),
                )
                .build()
        } else {
            navigateBack()
        }
    }

    private fun restoreFields(settings: AppSettings) {
        // используется для того, чтобы выставить initial'ы в филды
        maxDownloadsField.value = settings.maxDownloads
        ignoreServerErrorField.value = settings.ignoreServerError
        deleteUnfinishedField.value = settings.deleteUnfinished
        disableNotificationsField.value = settings.disableNotifications
    }

    companion object {

        const val DIALOG_TAG_CONFIRM = "confirm"

        const val KEY_FIELD_MAX_DOWNLOADS = "max_downloads"
        const val KEY_FIELD_IGNORE_SERVER_ERROR = "ignore_server_error"
        const val KEY_FIELD_DELETE_UNFINISHED = "delete_unfinished"
        const val KEY_FIELD_DISABLE_NOTIFICATIONS = "disable_notifications"
    }
}