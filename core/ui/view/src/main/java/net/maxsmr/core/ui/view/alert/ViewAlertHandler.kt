package net.maxsmr.core.ui.view.alert

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.ui.alert.AlertHandleInfoMap
import net.maxsmr.core.ui.alert.doHandleStandard
import net.maxsmr.core.ui.alert.representation.StandardAlertRepresentation

/**
 * Обработчик алертов на стороне UI (фрагментов/активити).
 * Нужен для того, чтобы закрыть все отображаемые диалоги в момент уничтожения фрагмента для
 * избежания утечки памяти.
 * Также при попытке установки фрагмента в качестве LifecycleOwner переключается на его ViewLifecycleOwner.
 */
@MainThread
class ViewAlertHandler(lifecycleOwner: LifecycleOwner) : DefaultLifecycleObserver {

    private val representationsMap =
        mutableMapOf<Pair<AlertQueue, String>, MutableList<Pair<Alert, StandardAlertRepresentation>>>()

    private var deferredHandles = mutableListOf<DeferredHandle>()

    private var owner: LifecycleOwner? = null
        set(value) {
            field?.lifecycle?.removeObserver(this)
            field = value
            value?.lifecycle?.addObserver(this)
        }

    init {
        if (lifecycleOwner is Fragment) {
            //При передаче фрагмента в качестве LifecycleOwner получаем его ViewLifecycleOwner,
            //т.к. показ алертов привязан имеено к ЖЦ вью, а не самого фрагмента
            try {
                //viewLifecycleOwner уже есть на момент создания AlertHandler
                owner = lifecycleOwner.viewLifecycleOwner
            } catch (e: IllegalStateException) {
                //viewLifecycleOwner нет, подписываемся на его появление и запоминаем все handle запросы, чтобы повторить позднее
                //по сути это перестраховка на случай создания AlertHandler до вызова onViewCreated
                lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
                    override fun onCreate(owner: LifecycleOwner) {
                        lifecycleOwner.viewLifecycleOwnerLiveData.observe(lifecycleOwner) { viewLifecycleOwner ->
                            //получили ViewLifecycleOwner, повторяем отложенные handle
                            this@ViewAlertHandler.owner = viewLifecycleOwner
                            viewLifecycleOwner.handleDeferred()
                        }
                    }
                })
            }
        } else {
            owner = lifecycleOwner
        }
    }

    fun handle(queue: AlertQueue, tag: String, representationFactory: (Alert) -> StandardAlertRepresentation?) {
        val owner = this.owner
        if (owner == null) {
            //Пока не можем обработать, запоминаем для обработки позднее
            deferredHandles.add(DeferredHandle(queue, tag, representationFactory))
            return
        }
        owner.doHandle(queue, tag, representationFactory)
    }

    private fun LifecycleOwner.handleDeferred() {
        val iterator = deferredHandles.iterator()
        while (iterator.hasNext()) {
            iterator.next().run { doHandle(queue, tag, representationFactory) }
            iterator.remove()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun LifecycleOwner.doHandle(
        queue: AlertQueue,
        tag: String,
        representationFactory: (Alert) -> StandardAlertRepresentation?,
    ) {
        (representationsMap as AlertHandleInfoMap).doHandleStandard(
            this,
            queue,
            tag,
            representationFactory
        )
    }

    override fun onDestroy(owner: LifecycleOwner) {
        representationsMap.values.forEach { representations ->
            representations.forEach {
                it.second.hide()
            }
        }
        representationsMap.clear()
    }

    private class DeferredHandle(
        val queue: AlertQueue,
        val tag: String,
        val representationFactory: (Alert) -> StandardAlertRepresentation?,
    )
}