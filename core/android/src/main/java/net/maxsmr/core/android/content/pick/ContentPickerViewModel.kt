package net.maxsmr.core.android.content.pick

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.delegates.persistableValue
import net.maxsmr.core.android.content.pick.concrete.ConcretePicker
import net.maxsmr.core.android.content.pick.concrete.ConcretePickerParams
import net.maxsmr.core.android.content.pick.concrete.ConcretePickerType
import javax.inject.Inject

@HiltViewModel
class ContentPickerViewModel @Inject constructor(state: SavedStateHandle) : BaseViewModel(state) {

    val pickResultEvent: StateFlow<VmEvent<PickResult>?> by lazy {
        _pickResultEvent.asStateFlow()
    }

    val appChoicesEvent: StateFlow<VmEvent<AppChoice>?> by lazy {
        _appChoicesEvent.asStateFlow()
    }

    /**
     * Эмитит результаты взятия контента
     */
    private val _pickResultEvent = MutableStateFlow<VmEvent<PickResult>?>(null)

    /**
     * Эмитит события выбора юзером конкретного приложения для взятия контента
     */
    private val _appChoicesEvent = MutableStateFlow<VmEvent<AppChoice>?>(null)

    /**
     * Хранит тип [ConcretePickerType], используемого для взятия контента. При получении результата
     * используется, чтобы понять, какому [ConcretePicker] делегировать обработку
     */
    var selectedPickerType: ConcretePickerType? by persistableValue()

    fun onSuccess(requestCode: Int, uri: Uri, pickerType: ConcretePickerType) {
        _pickResultEvent.tryEmit(VmEvent(PickResult.Success(requestCode, uri, pickerType)))
    }

    fun onError(requestCode: Int, errorMessage: TextMessage, exception: Throwable? = null) {
        _pickResultEvent.tryEmit(VmEvent(PickResult.Error(requestCode, errorMessage, exception)))
    }

    fun onAppChoice(choice: AppChoice) {
        _appChoicesEvent.tryEmit(VmEvent(choice))
    }

    /**
     * Данные выбранного пользователем приложения для взятия контента
     */
    class AppChoice(
        val requestCode: Int,
        val params: ConcretePickerParams,
        val intentWithPermissions: IntentWithPermissions,
    )
}