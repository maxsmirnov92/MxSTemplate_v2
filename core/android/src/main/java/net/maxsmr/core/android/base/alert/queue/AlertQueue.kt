package net.maxsmr.core.android.base.alert.queue

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.queue.AlertQueueItem.UniqueStrategy
import java.util.*

/**
 * Очередь оповещений с приоритетами.
 * Приоритет сообщения в голове очереди искусственно повышается до максимального. Таким образом новые
 * сообщения, поступающие в очередь, не меняют голову очереди, а встают за ней согласно приоритету.
 * Но в случае необходимости новое сообщение можно поместить в голову очереди (см. [AlertQueueItem.Builder.setPriority])
 *
 * @param sortOrder устанавливает очередность сообщений в случае одинакового [AlertQueueItem.priority]
 */
class AlertQueue(
    val sortOrder: SortOrder = SortOrder.OLDER_BEFORE,
) {
    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("AlertQueue")

    private val queue: LiveQueue<AlertQueueItem> = LiveQueue(PriorityQueue(4))

    init {
        queue.asLiveData().observeForever {
            if (queue.isNotEmpty()) {
                logger.d("AlertQueue->${toString()}")
            }
            //искусственно повышаем приоритет сообщения в голове очереди, чтобы оно не перебивалось новыми
            it?.setMaxPriority()
        }
    }

    /**
     * Эмитит сообщения с тэгом [tag] либо null, если очередь пуста или в голове находится сообщение
     * с другим тэгом
     */
    fun asLiveData(tag: String): LiveData<Alert?> = queue.asLiveData()
            .map { it?.takeIf { it.tag == tag }?.alert }

    fun isEmpty() = queue.isEmpty()

    fun hasTag(tag: String) = getFirstByTag(tag) != null

    fun getFirstByTag(tag: String): AlertQueueItem? = getAllByTag(tag).firstOrNull()

    fun getAllByTag(tag: String): List<AlertQueueItem> = queue.filter { it.tag == tag }

    /**
     * Добавляет элемент в очередь
     */
    fun add(item: AlertQueueItem): Boolean {
        val head = queue.peek()
        if (item == head) return false

        if (item.unique == UniqueStrategy.Ignore && getFirstByTag(item.tag) != null) {
            return false
        }

        if (item.unique is UniqueStrategy.Custom && !item.unique.lambda(this)) {
            return false
        }

        if (item.putInQueueHead) {
            head?.resetMaxPriority()
            item.setMaxPriority()
        }
        return queue.add(item).also {
            if (item.unique == UniqueStrategy.Replace) removeWithSameTag(item)
        }
    }

    fun remove(item: AlertQueueItem): Boolean = queue.remove(item)

    /**
     * Удаляет все сообщения с тэгом [tag] из этой очереди
     */
    fun removeAllWithTag(tag: String) {
        val iterator = queue.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.tag == tag) {
                iterator.remove()
            }
        }
    }

    /**
     * Удаляет из очереди все сообщения с тэгом сообщения [item], кроме самого [item]
     */
    private fun removeWithSameTag(item: AlertQueueItem) {
        val iterator = queue.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next !== item && next.tag == item.tag) {
                iterator.remove()
            }
        }
    }

    override fun toString(): String {
        return queue.joinToString(separator = "->") { it.toString() }
    }

    /**
     * Устанавливает очередность сообщений с одинаковым [AlertQueueItem.priority].
     *
     * @param k коэффициент для сортировки
     */
    @Suppress("unused")
    enum class SortOrder(val k: Int) {

        /**
         * Более старые сообщения помещаются ближе к голове очереди
         */
        OLDER_BEFORE(1),

        /**
         * Более новые сообщения помещаются ближе к голове очереди
         */
        NEWER_BEFORE(-1)
    }
}