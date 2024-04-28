package net.maxsmr.core.android.base.alert.queue

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import net.maxsmr.commonutils.live.setValueIfNew
import java.util.*

/**
 * Очередь с дополнительной фичей - LiveData, эмитящая головной элемент очереди
 */
class LiveQueue<E>(
        private val queue: Queue<E>
) : Queue<E> by queue {

    private val headLiveData: MutableLiveData<E?> by lazy { MutableLiveData(peek()) }

    fun asLiveData(): LiveData<E?> = headLiveData

    override fun add(element: E): Boolean {
        return queue.add(element).notifyChange()
    }

    override fun addAll(elements: Collection<E>): Boolean {
        return queue.addAll(elements).notifyChange()
    }

    override fun clear() {
        queue.clear().notifyChange()
    }

    override fun remove(element: E): Boolean {
        return queue.remove(element).notifyChange()
    }

    override fun remove(): E {
        return queue.remove().notifyChange()
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        return queue.removeAll(elements).notifyChange()
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        return queue.retainAll(elements).notifyChange()
    }

    override fun offer(element: E): Boolean {
        return queue.offer(element).notifyChange()
    }

    override fun poll(): E? {
        return queue.poll().notifyChange()
    }

    override fun iterator(): MutableIterator<E> {
        return NotifyIterator(queue.iterator())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LiveQueue<*>) return false

        if (queue != other.queue) return false

        return true
    }

    override fun hashCode(): Int {
        return queue.hashCode()
    }

    private fun <T> T.notifyChange(): T = this.also { headLiveData.setValueIfNew(peek()) }

    private inner class NotifyIterator<E>(
            private val src: MutableIterator<E>
    ) : MutableIterator<E> by src {

        override fun remove() {
            src.remove().notifyChange()
        }
    }
}