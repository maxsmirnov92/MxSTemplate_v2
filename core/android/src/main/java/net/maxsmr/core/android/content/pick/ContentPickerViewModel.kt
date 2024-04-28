package net.maxsmr.core.android.content.pick

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.commonutils.live.event.VmEvent
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.delegates.persistableValue
import net.maxsmr.core.android.content.pick.concrete.ConcretePickerParams
import net.maxsmr.core.android.content.pick.concrete.ConcretePicker
import net.maxsmr.core.android.content.pick.concrete.ConcretePickerType

class ContentPickerViewModel(
    state: SavedStateHandle,
) : BaseViewModel(state) {

    /**
     * Эмитит результаты взятия контента
     */
    val pickResult: MutableLiveData<VmEvent<PickResult>> = MutableLiveData()

    /**
     * Эмитит события выбора юзером конкретного приложения для взятия контента
     */
    val appChoices: MutableLiveData<VmEvent<AppChoice>> = MutableLiveData()

    /**
     * Хранит тип [ConcretePickerType], используемого для взятия контента. При получении результата
     * используется, чтобы понять, какому [ConcretePicker] делегировать обработку
     */
    var selectedPickerType: ConcretePickerType? by persistableValue()

    fun onSuccess(requestCode: Int, uri: Uri, pickerType: ConcretePickerType) {
        pickResult.postValue(VmEvent(PickResult.Success(requestCode, uri, pickerType)))
    }

    fun onError(requestCode: Int, errorMessage: TextMessage, exception: Throwable? = null) {
        pickResult.postValue(VmEvent(PickResult.Error(requestCode, errorMessage, exception)))
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