package net.maxsmr.core.android.base.alert.queue

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import net.maxsmr.commonutils.live.setValueIfNew
import java.util.Queue

/**
 * Очередь с дополнительной фичей - LiveData, эмитящая головной элемент очереди
 */
@MainThread
class LiveQueue<E : LiveQueue.QueueItem>(
    private val queue: Queue<E>,
) : Queue<E> by queue {

    private val headLiveData: MutableLiveData<E?> by lazy { MutableLiveData(peek()) }

    private var shouldNotify = true

    fun asLiveData(): LiveData<E?> = headLiveData

    override fun add(element: E): Boolean {
        return queue.add(element).also {
            if (it) {
                applyToHead()
            }
        }
    }

    override fun addAll(elements: Collection<E>): Boolean {
        return queue.addAll(elements).also {
            if (it) {
                applyToHead()
            }
        }
    }

    override fun clear() {
        queue.clear()
        applyToHead()
    }

    override fun remove(): E {
        return queue.remove().also {
            applyToHead()
        }
    }

    override fun remove(element: E): Boolean {
        return queue.remove(element).also {
            if (it) {
                applyToHead()
            } else {
                // изменений в очереди не было
                // (возможно, при isOneShot этот итем уже был убран)
                // вручную убрать из headLiveData, если совпадает
                val headValue = headLiveData.value
                if (headValue == element) {
                    headLiveData.value = null
                }
            }
        }
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        return queue.removeAll(elements.toSet()).also {
            if (it) {
                applyToHead()
            } else {
                val headValue = headLiveData.value
                if (elements.any { e -> e == headValue }) {
                    headLiveData.value = null
                }
            }
        }
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        return queue.retainAll(elements.toSet()).also {
            if (it) {
                applyToHead()
            }
        }
    }

    override fun offer(element: E): Boolean {
        return queue.offer(element).also {
            if (it) {
                applyToHead()
            }
        }
    }

    override fun poll(): E? {
        return queue.poll().also {
            if (it != null) {
                applyToHead()
            }
        }
    }

    override fun iterator(): MutableIterator<E> {
        return NotifyIterator(queue.iterator())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LiveQueue<*>) return false

        return queue == other.queue
    }

    override fun hashCode(): Int {
        return queue.hashCode()
    }

    fun removeFromHeadIf(lambda: (E) -> Boolean) {
        val headValue = headLiveData.value
        if (headValue != null && lambda(headValue)) {
            headLiveData.setValueIfNew(null)
        }
    }

    private fun applyToHead()  {
        val peekValue = peek()
        if (peekValue != null && peekValue.isOneShot) {
            shouldNotify = false
            remove()
            shouldNotify = true
        }
        if (shouldNotify) {
            // не использовать post, т.к. кто-то может успеть вмешаться,
            // например, при dismiss
            headLiveData.setValueIfNew(peekValue)
        }
    }

    interface QueueItem {

        val isOneShot: Boolean
    }

    private inner class NotifyIterator<E>(
        private val src: MutableIterator<E>,
    ) : MutableIterator<E> by src {

        override fun remove() {
            src.remove()
            applyToHead()
        }
    }
}