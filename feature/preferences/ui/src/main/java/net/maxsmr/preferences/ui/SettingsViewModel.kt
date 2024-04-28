package net.maxsmr.preferences.ui

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
import net.maxsmr.preferences.domain.AppSettings
import net.maxsmr.preferences.repository.SettingsDataStoreRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsDataStoreRepository,
    state: SavedStateHandle
): BaseViewModel(state) {

    val maxDownloadsField: Field<Int> = Field.Builder(0)
        .emptyIf { false }
        .hint(R.string.settings_hint_field_max_downloads)
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
        } else {
            appSettings.observeOnce {
                restoreFields(it)
            }
        }
        maxDownloadsField.valueLive.observe {
            appSettings.value = currentAppSettings.copy(maxDownloads = it)
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
    }

    companion object {

        const val DIALOG_TAG_CONFIRM = "confirm"
    }
}