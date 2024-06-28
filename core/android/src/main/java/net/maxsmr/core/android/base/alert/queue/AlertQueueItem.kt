package net.maxsmr.core.android.base.alert.queue

import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.BaseViewModel
import net.maxsmr.core.android.base.alert.Alert

/**
 * Элемент очереди сообщений, неразрывно связан с очередью. Является оберткой над [Alert] с
 * дополнительной информацией о том, как алерт должен позиционироваться в очереди.
 *
 * @param tag служебный тэг для возможности сравнения алертов
 * @param queue очередь, в которую сразу при создании помещается этот элемент
 * @param alert данные сообщения
 * @param initialPriority приоритет сообщения. Чем больше, тем выше.
 * @param putInQueueHead признак того, что при добавлении в очередь сообщение нужно принудительно поместить в голову очереди.
 * @param unique стратегия обработки уникальных сообщений
 */
class AlertQueueItem private constructor(
    val tag: String,
    val queue: AlertQueue,
    val alert: Alert,
    private val initialPriority: Int,
    val putInQueueHead: Boolean,
    val unique: UniqueStrategy,
    override val isOneShot: Boolean = false
) : LiveQueue.QueueItem, Comparable<AlertQueueItem> {

    /**
     * Время создания сообщения для сортировки при одинаковом [initialPriority]
     */
    private val timestamp: Long = System.currentTimeMillis()

    private var priority: Int = initialPriority

    /**
     * Повышает приоритет сообщения до максимального. Используется, когда сообщение попадает в голову очереди.
     */
    fun setMaxPriority() {
        priority = Priority.HIGHEST.value + 1
    }

    /**
     * Восстанавливает первоначальный приоритет сообщения, с которым оно было создано.
     * Используется, когда сообщение отодвигается из головы очереди при добавлении нового сообщения в голову очереди
     */
    fun resetMaxPriority() {
        priority = initialPriority
    }

    override fun compareTo(other: AlertQueueItem): Int = when {
        priority < other.priority -> 1
        priority > other.priority -> -1
        timestamp > other.timestamp -> 1 * queue.sortOrder.k
        timestamp < other.timestamp -> -1 * queue.sortOrder.k
        else -> 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AlertQueueItem) return false

        if (tag != other.tag) return false
        if (alert != other.alert) return false
        if (putInQueueHead != other.putInQueueHead) return false
        if (unique != other.unique) return false
        if (unique != UniqueStrategy.Replace) {
            //Если элемент уникальный, то в очереди только 1, и при сравнении приоритет не важен
            if (priority != other.priority) return false
            if (timestamp != other.timestamp) return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = tag.hashCode()
        result = 31 * result + alert.hashCode()
        result = 31 * result + putInQueueHead.hashCode()
        result = 31 * result + unique.hashCode()
        if (unique != UniqueStrategy.Replace) {
            result = 31 * result + timestamp.hashCode()
            result = 31 * result + priority
        }
        return result
    }

    override fun toString(): String = tag

    @Suppress("unused")
    enum class Priority(val value: Int) {

        LOWEST(-20),
        LOW(-10),
        NORMAL(0),
        HIGH(10),
        HIGHEST(20);
    }

    sealed class UniqueStrategy {
        /**
         * Сообщение не уникально (может быть более 1 сообщения с одинаковым тэгом в очереди)
         */
        object None : UniqueStrategy()

        /**
         * Добавляемое сообщение не добавляется, если в очереди уже есть сообщение с таким же тегом
         */
        object Ignore : UniqueStrategy()

        /**
         * Добавляемое сообщение заменяет все добавленные в очередь ранее с таким же тегом
         */
        object Replace : UniqueStrategy()

        /**
         * Решение о попадании сообщения в очередь принимается в [lambda]
         *
         * @param lambda возвращает true, если алерт требуется добавить в очередь, иначе false
         */
        class Custom(val lambda: (AlertQueue) -> Boolean) : UniqueStrategy()
    }

    open class Builder private constructor(
        private val tag: String,
        private val queue: AlertQueue,
        private val builder: Alert.Builder,
    ) : Alert.IBuilder<AlertQueueItem> {

        /**
         * Конструктор для внутреннего использования.
         * При вызове из фрагмента может приводить к утечке памяти;
         * Вместо этого используйте соответствующий билдер [BaseViewModel]
         */
        internal constructor(tag: String, queue: AlertQueue) : this(tag, queue, Alert.Builder())

        private var priority: Int = Priority.NORMAL.value
        private var uniqueStrategy: UniqueStrategy = UniqueStrategy.None
        private var putInQueueHead: Boolean = false
        private var isOneShot: Boolean = true
        private var onClose: (() -> Unit)? = null

        override fun setTitle(title: String?): Builder = apply {
            builder.setTitle(title)
        }

        override fun setTitle(title: Int?): Builder = apply {
            builder.setTitle(title)
        }

        override fun setTitle(title: TextMessage?): Builder = apply {
            builder.setTitle(title)
        }

        override fun setMessage(message: CharSequence?): Builder = apply {
            builder.setMessage(message)
        }

        override fun setMessage(message: Int?): Builder = apply {
            builder.setMessage(message)
        }

        override fun setMessage(message: TextMessage?): Builder = apply {
            builder.setMessage(message)
        }

        override fun setAnswers(answers: List<Alert.Answer>): Builder = apply {
            builder.setAnswers(answers)
        }

        override fun setAnswers(vararg answers: Alert.Answer): Builder = apply {
            builder.setAnswers(*answers)
        }

        override fun setExtraData(extraData: Any?): Builder = apply {
            builder.setExtraData(extraData)
        }

        /**
         * Задает приоритет сообщения
         *
         * @param priority приоритет
         * @param putInQueueHead true, если необходимо поместить сообщение в голову очереди, иначе false.
         * Флаг оказывает влияние только в момент добавление сообщения в очередь (позволяет "влезть без очереди").
         * После добавления порядок сообщений регулируется только приоритетом. В большинстве случаев
         * следует использовать значение по умолчанию.
         */
        fun setPriority(priority: Priority, putInQueueHead: Boolean = false) = apply {
            this.priority = if (priority == Priority.HIGHEST) priority.value - 1 else priority.value
            this.putInQueueHead = putInQueueHead
        }

        /**
         * Устанавливает стратегию уникальности, применяемую при добавлении сообщения в очередь
         */
        fun setUniqueStrategy(uniqueStrategy: UniqueStrategy) = apply {
            this.uniqueStrategy = uniqueStrategy
        }

        fun setOneShot(isOneShot: Boolean) = apply {
            this.isOneShot = isOneShot
        }

        fun setOnClose(onClose: (() -> Unit)) = apply {
            this.onClose = onClose
        }

        /**
         * Создает сообщение и добавляет его в очередь
         *
         * @return созданное сообщение, или null, если сообщение не было добавлено в очередь.
         * Может быть использовано для удаления из очереди в дальнейшем
         */
        override fun build(): AlertQueueItem? {
            check(tag.isNotEmpty()) {
                "Alert tag must be specified"
            }
            var item: AlertQueueItem? = null
            val alert = builder
                .onClose {
                    item?.let {
                        queue.remove(it)
                    }
                    onClose?.invoke()
                }
                .build()
            alert.answers.forEach {
                val internalSelect = it.select
                it.select = {
                    if (it.closeAfterSelect) {
                        alert.close()
                    }
                    internalSelect?.invoke()
                }
            }
            item = AlertQueueItem(
                tag = tag,
                queue = queue,
                alert = alert,
                initialPriority = priority,
                putInQueueHead = putInQueueHead,
                unique = uniqueStrategy,
                isOneShot = isOneShot
            )
            return item.takeIf { queue.add(it) }
        }
    }
}