package net.maxsmr.feature.notification_reader.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.datetime.Instant
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.feature.notification_reader.data.NotificationReaderListenerService
import net.maxsmr.feature.notification_reader.data.NotificationReaderRepository
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager
import net.maxsmr.feature.notification_reader.data.NotificationReaderSyncManager.StartMode
import net.maxsmr.feature.notification_reader.ui.adapter.NotificationsAdapterData
import javax.inject.Inject

@HiltViewModel
class NotificationReaderViewModel @Inject constructor(
    private val syncManager: NotificationReaderSyncManager,
    repo: NotificationReaderRepository,
    state: SavedStateHandle,
) : BaseHandleableViewModel(state) {

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

    fun onToggleServiceTargetStateAction() {
        serviceTargetState.value = !isServiceRunning()
    }

    fun onDownloadPackageListAction() {
        if (!syncManager.doLaunchDownloadJobIfNeeded(
                    if (serviceTargetState.value == true) {
                        StartMode.JOBS_AND_SERVICE
                    } else {
                        StartMode.NONE
                    }
                )
        ) {
            showSnackbar(TextMessage(R.string.notification_reader_snack_download_package_list_not_started))
        }
    }

    fun isServiceRunning(): Boolean {
        return NotificationReaderListenerService.isRunning() /*&& serviceTargetState.value != false*/
    }
}