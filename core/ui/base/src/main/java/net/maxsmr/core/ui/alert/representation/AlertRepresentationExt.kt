package net.maxsmr.core.ui.alert.representation

import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.ui.alert.representation.AlertRepresentationResult.HideAlerts
import net.maxsmr.core.ui.alert.representation.AlertRepresentationResult.ShowAlert

fun <AR : AlertRepresentation> handleAlertRepresentation(
    alertInfo: AlertQueue.AlertInfo?,
    representationHolder: AlertRepresentationHolder<AR>,
): List<AlertRepresentationResult> {
    val result = mutableListOf<AlertRepresentationResult>()

    if (alertInfo == null || alertInfo.isReplaceable) {
        // требуется скрытие при наличии каких-либо в данный момент,
        // т.к. по данному тэгу ничего нет (alertInfo нульный)
        // или новый должен заменить текущий(е) при наличии с таким тэгом
        representationHolder.clear()
            .takeIf { it.isNotEmpty() }
            ?.let { result.add(HideAlerts(it)) }
    }

    if (alertInfo != null) {
        representationHolder.remove(alertInfo.alert)?.let {
            result.add(HideAlerts(listOf(it)))
        }
        // новый алерт для показа
        result.add(
            ShowAlert(
                representationHolder.getOrCreate(alertInfo.alert)
            )
        )
    }

    return result
}