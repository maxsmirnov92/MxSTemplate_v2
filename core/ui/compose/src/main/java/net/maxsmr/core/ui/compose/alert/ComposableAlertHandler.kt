package net.maxsmr.core.ui.compose.alert

import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.ui.alert.representation.AlertRepresentationHolder
import net.maxsmr.core.ui.alert.representation.AlertRepresentationResult
import net.maxsmr.core.ui.alert.representation.handleAlertRepresentation
import net.maxsmr.core.ui.compose.alert.representation.ComposableAlertRepresentation

/**
 * Предназначен для обработки сообщений из [AlertQueue] с целью:
 * вызова Composable-функций отображения для [ComposableAlertRepresentation]
 * В кач-ве [lifecycleOwner] предполагается Activity
 */
@MainThread
class ComposableAlertHandler  {

    @Suppress("UNCHECKED_CAST")
    @Composable
    fun Handle(
        queue: AlertQueue,
        tag: String,
        representationFactory: (Alert) -> ComposableAlertRepresentation,
    ) {
        val alertInfo by queue.asLiveData(tag).observeAsState()
        val result = handleAlertRepresentation(alertInfo,
            AlertRepresentationHolder(representationFactory)
        )
        result.forEach {
            (it as? AlertRepresentationResult.ShowAlert<ComposableAlertRepresentation>)?.representation?.Show()
        }
    }
}