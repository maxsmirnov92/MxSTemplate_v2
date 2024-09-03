package net.maxsmr.feature.preferences.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.live.field.Field
import net.maxsmr.commonutils.live.field.validateAndSetByRequiredFields
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.delegates.persistableLiveData
import net.maxsmr.core.android.base.delegates.persistableValue
import net.maxsmr.core.domain.entities.feature.address_sorter.routing.RoutingApp
import net.maxsmr.core.domain.entities.feature.settings.AppSettings
import net.maxsmr.core.ui.alert.AlertFragmentDelegate
import net.maxsmr.core.ui.alert.representation.asYesNoNeutralDialog
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.feature.preferences.data.repository.SettingsDataStoreRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsDataStoreRepository,
    state: SavedStateHandle,
) : BaseHandleableViewModel(state) {

    val routingAppField: Field<RoutingApp> = Field.Builder(RoutingApp.DOUBLEGIS)
        .emptyIf { false }
        .persist(state, KEY_FIELD_ROUTING_APP)
        .build()

    val routingAppFromCurrentField: Field<Boolean> = Field.Builder(false)
        .emptyIf { false }
        .persist(state, KEY_FIELD_ROUTING_APP_FROM_CURRENT)
        .build()

    private val allFields = listOf<Field<*>>(
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

        const val KEY_FIELD_ROUTING_APP = "routing_app"
        const val KEY_FIELD_ROUTING_APP_FROM_CURRENT = "routing_app_from_current"
    }
}