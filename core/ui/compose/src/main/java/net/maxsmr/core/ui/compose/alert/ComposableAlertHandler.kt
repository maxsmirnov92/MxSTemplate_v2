package net.maxsmr.core.ui.compose.alert

import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.ui.alert.doHandle
import net.maxsmr.core.ui.alert.doHandleStandard
import net.maxsmr.core.ui.alert.representation.AlertRepresentation
import net.maxsmr.core.ui.alert.representation.StandardAlertRepresentation
import net.maxsmr.core.ui.compose.alert.representation.ComposableAlertRepresentation

/**
 * Предназначен для обработки сообщений из [AlertQueue] с целью:
 * 1. Вызова Composable-функций отображения для [ComposableAlertRepresentation]
 * 2. Стандартного отображения/скрытия для [StandardAlertRepresentation].
 * В кач-ве [lifecycleOwner] предполагается Activity
 */
@MainThread
class ComposableAlertHandler(private val lifecycleOwner: LifecycleOwner) : DefaultLifecycleObserver {

    private val representationsMap =
        mutableMapOf<Pair<AlertQueue, String>, MutableList<Pair<Alert, AlertRepresentation>>>()

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    @Composable
    fun HandleComposable(
        queue: AlertQueue,
        tag: String,
        representationFactory: (Alert) -> ComposableAlertRepresentation?,
    ) {
        val alertInfo = queue.asLiveData(tag).observeAsState().value
        val shouldShowRepresentation = representationsMap.doHandle<ComposableAlertRepresentation>(
            alertInfo,
            queue,
            tag,
            representationFactory
        ).observeAsState().value
        if (shouldShowRepresentation != null && shouldShowRepresentation.second) {
            shouldShowRepresentation.first.Show()
        }
    }

    fun handleStandard(
        queue: AlertQueue,
        tag: String,
        representationFactory: (Alert) -> StandardAlertRepresentation?,
    ) {
        representationsMap.doHandleStandard(
            lifecycleOwner,
            queue,
            tag,
            representationFactory
        )
    }

    override fun onDestroy(owner: LifecycleOwner) {
        representationsMap.clear()
    }
}