package net.maxsmr.core.android.base.alert

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.android.base.alert.representation.AlertRepresentation

/**
 * Обработчик алертов на стороне UI (фрагментов/активити).
 * Нужен для того, чтобы закрыть все отображаемые диалоги в момент уничтожения фрагмента для
 * избежания утечки памяти.
 * Также при попытке установки фрагмента в качестве LifecycleOwner переключается на его ViewLifecycleOwner.
 */
@MainThread
class AlertHandler(
    lifecycleOwner: LifecycleOwner,
) : DefaultLifecycleObserver {

    private val representations: MutableList<AlertRepresentation> = mutableListOf()
    private var deferredHandles: MutableList<DeferredHandle> = mutableListOf()
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
                            this@AlertHandler.owner = viewLifecycleOwner
                            viewLifecycleOwner.handleDeferred()
                        }
                    }
                })
            }
        } else {
            owner = lifecycleOwner
        }
    }

    fun handle(queue: AlertQueue, tag: String, representationFactory: (Alert) -> AlertRepresentation?) {
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

    private fun LifecycleOwner.doHandle(
        queue: AlertQueue,
        tag: String,
        representationFactory: (Alert) -> AlertRepresentation?,
    ) {
        var representation: AlertRepresentation? = null
        queue.asLiveData(tag).observe(this) { alert ->
            representation?.let {
                it.hide()
                representations.remove(it)
            }
            alert?.let(representationFactory)?.also {
                representation = it
                representations.add(it)
                it.show()
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        representations.forEach { it.hide() }
    }

    private class DeferredHandle(
        val queue: AlertQueue,
        val tag: String,
        val representationFactory: (Alert) -> AlertRepresentation?,
    )
}