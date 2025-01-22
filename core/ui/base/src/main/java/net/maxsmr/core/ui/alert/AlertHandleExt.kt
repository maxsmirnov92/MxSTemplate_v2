package net.maxsmr.core.ui.alert

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import net.maxsmr.commonutils.live.unsubscribeIf
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.ui.alert.representation.AlertRepresentation
import net.maxsmr.core.ui.alert.representation.StandardAlertRepresentation

typealias AlertHandleInfoMap = MutableMap<Pair<AlertQueue, String>, MutableList<Pair<Alert, AlertRepresentation>>>

fun AlertHandleInfoMap.doHandleStandard(
    lifecycleOwner: LifecycleOwner,
    queue: AlertQueue,
    tag: String,
    representationFactory: (Alert) -> StandardAlertRepresentation?,
) {
    queue.asLiveData(tag).observe(lifecycleOwner) { alertInfo ->
        val shouldShowRepresentation = doHandle<StandardAlertRepresentation>(
            alertInfo,
            queue,
            tag,
            representationFactory
        )
        shouldShowRepresentation
            .unsubscribeIf { !it.second }
            .observe(lifecycleOwner) {
                if (it.second) {
                    it.first.show()
                } else {
                    it.first.hide()
                }
            }
    }
}

inline fun <reified AR : AlertRepresentation> AlertHandleInfoMap.doHandle(
    alertInfo: AlertQueue.AlertInfo?,
    queue: AlertQueue,
    tag: String,
    representationFactory: (Alert) -> AlertRepresentation?,
): LiveData<Pair<AR, Boolean>> {
    val result = MutableLiveData<Pair<AR, Boolean>>()

    val key = Pair(queue, tag)
    val representations = this[key] ?: mutableListOf()
    if (alertInfo == null || alertInfo.isReplaceable) {
        // верхний при данном тэге нульный
        // или текущий alert предполагает удаление остальных с тем же тэгом
        representations.forEach {
            val rep = it.second
            if (rep is AR) {
                result.value = rep to false
            }
        }
        representations.clear()
    }

    val newAlert = alertInfo?.alert
    if (!representations.any { it.first === newAlert }) {
        // в observer возможно попадание с последним алертом,
        // по которому уже был вызван show в данном AlertHandler;
        // проверка по ссылке, т.к. содержимое может совпадать
        newAlert?.let(representationFactory)?.also {
            representations.add(Pair(newAlert, it))
            if (it is AR) {
                result.value = it to true
            }
        }
    }
    this[key] = representations

    return result
}