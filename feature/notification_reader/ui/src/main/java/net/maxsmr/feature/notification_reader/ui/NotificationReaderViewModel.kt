package net.maxsmr.feature.notification_reader.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import javax.inject.Inject

@HiltViewModel
class NotificationReaderViewModel @Inject constructor(

    state: SavedStateHandle
): BaseHandleableViewModel(state) {

    val serviceTargetState = MutableLiveData(true)

    fun toggleServiceTargetState() {
        serviceTargetState.value = !(serviceTargetState.value ?: false)
    }


}