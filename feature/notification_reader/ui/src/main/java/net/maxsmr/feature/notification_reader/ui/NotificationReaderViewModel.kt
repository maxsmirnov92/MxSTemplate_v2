package net.maxsmr.feature.notification_reader.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.feature.notification_reader.data.NotificationReaderListenerService
import javax.inject.Inject

@HiltViewModel
class NotificationReaderViewModel @Inject constructor(

    state: SavedStateHandle
): BaseHandleableViewModel(state) {

    val serviceTargetState = MutableLiveData(true)

    fun toggleServiceTargetState() {
        serviceTargetState.value = !isServiceRunning()
    }

    fun isServiceRunning(): Boolean {
        return NotificationReaderListenerService.isRunning() && serviceTargetState.value != false
    }
}