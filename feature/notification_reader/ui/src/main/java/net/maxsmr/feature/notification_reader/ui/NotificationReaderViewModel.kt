package net.maxsmr.feature.notification_reader.ui

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.coroutines.usecase.UseCaseResult
import net.maxsmr.core.ui.components.BaseHandleableViewModel
import net.maxsmr.feature.notification_reader.data.NotificationReaderKeyImportUseCase
import javax.inject.Inject

@HiltViewModel
class NotificationReaderViewModel @Inject constructor(
    private val keyImportUseCase: NotificationReaderKeyImportUseCase,
    state: SavedStateHandle
): BaseHandleableViewModel(state) {

    val serviceTargetState = MutableLiveData(true)

    fun toggleServiceTargetState() {
        serviceTargetState.value = !(serviceTargetState.value ?: false)
    }

    fun onPickApiKeyFromFile(uri: Uri) {
        dialogQueue.toggle(true, DIALOG_TAG_PROGRESS)
        viewModelScope.launch {
            val result = keyImportUseCase.invoke(uri)
            dialogQueue.toggle(false, DIALOG_TAG_PROGRESS)
            if (result is UseCaseResult.Error) {
                showOkDialog(
                    DIALOG_TAG_IMPORT_FAILED,
                    result.errorMessage()?.let {
                        TextMessage(R.string.notification_reader_dialog_key_import_error_message_format, it)
                    } ?: TextMessage(R.string.notification_reader_dialog_key_import_error_message)
                )
            }
        }
    }

    companion object {

        const val DIALOG_TAG_IMPORT_FAILED = "import_failed"
    }
}