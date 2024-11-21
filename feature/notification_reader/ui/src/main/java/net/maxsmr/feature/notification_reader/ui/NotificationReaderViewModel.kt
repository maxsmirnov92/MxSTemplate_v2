package net.maxsmr.feature.notification_reader.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import dagger.hilt.android.lifecycle.HiltViewModel
import net.maxsmr.core.android.base.BaseViewModel
import kotlinx.datetime.Instant
import net.maxsmr.feature.notification_reader.data.NotificationReaderListenerService

import net.maxsmr.feature.notification_reader.data.NotificationReaderRepository
import net.maxsmr.feature.notification_reader.ui.adapter.NotificationsAdapterData
import javax.inject.Inject

@HiltViewModel
class NotificationReaderViewModel @Inject constructor(
    repo: NotificationReaderRepository,
    state: SavedStateHandle,
) : BaseViewModel(state) {

    val serviceTargetState = MutableLiveData(true)

    val notificationsItems = repo.getNotifications().asLiveData().map { list ->
        list.map {
            NotificationsAdapterData(
                it.id,
                it.contentText,
                it.packageName,
                Instant.fromEpochMilliseconds(it.timestamp).toString(),
                it.status
            )
        }
    }

    fun toggleServiceTargetState() {
        serviceTargetState.value = !isServiceRunning()
    }

    fun isServiceRunning(): Boolean {
        return NotificationReaderListenerService.isRunning() && serviceTargetState.value != false
    }
}