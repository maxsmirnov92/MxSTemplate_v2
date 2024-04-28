package net.maxsmr.core.android.base.alert

import androidx.annotation.StringRes
import net.maxsmr.commonutils.gui.message.TextMessage
import net.maxsmr.core.android.base.alert.queue.AlertQueue
import net.maxsmr.core.android.base.alert.queue.AlertQueueItem

/**
 * Данные сообщения пользователю, абстрагированные от формы представления (может использоваться любая).
 * Сам класс не связан с очередью сообщений, но в приложении преимущественно предполагается
 * использование этого класса в обертке [AlertQueueItem], которая тесно связана с [AlertQueue].
 *
 * @see AlertRepresentation
 *
 * @param title заголовок алерта
 * @param message опциональное описание алерта
 * @param answers список возможных ответов
 * @param extraData дополнительные, опциональные данные произвольного типа
 * @param onClose действие по закрытию алерта
 */
class Alert private constructor(
    val title: TextMessage?,
    val message: TextMessage?,
    val answers: List<Answer>,
    val extraData: Any?,
    private val onClose: (() -> Unit)?,
) {

    fun close() {
        onClose?.invoke()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Alert) return false

        if (title != other.title) return false
        if (message != other.message) return false
        if (answers != other.answers) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + (message?.hashCode() ?: 0)
        result = 31 * result + answers.hashCode()
        return result
    }

    /**
     * Ответ на вопрос
     *
     * @param title текст ответа на вопрос
     */
    class Answer(
        val title: TextMessage,
        val tag: Any? = null,
        val contentDescription: TextMessage? = null
    ) {

        /**
         * Метод для выбора этого ответа на вопрос. Задается на стороне, создающей этот вариант ответа
         */
        var select: (() -> Unit)? = null
            internal set

        var closeAfterSelect: Boolean = true

        constructor(@StringRes titleRes: Int) : this(TextMessage(titleRes))

        constructor(title: String) : this(TextMessage(title))

        fun onSelect(closeAfterSelect: Boolean = true, action: () -> Unit) = apply {
            this.select = action
            this.closeAfterSelect = closeAfterSelect
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Answer) return false

            if (title != other.title) return false

            return true
        }

        override fun hashCode(): Int {
            return title.hashCode()
        }

        companion object {

            fun Collection<Answer>.findByTag(tag: Any) = find { it.tag == tag }
        }
    }

    interface IBuilder<out T> {

        fun setTitle(title: String?): IBuilder<T>

        fun setTitle(@StringRes title: Int?): IBuilder<T>

        fun setTitle(title: TextMessage?): IBuilder<T>

        fun setMessage(message: CharSequence?): IBuilder<T>

        fun setMessage(@StringRes message: Int?): IBuilder<T>

        fun setMessage(message: TextMessage?): IBuilder<T>

        fun setAnswers(answers: List<Answer>): IBuilder<T>

        fun setAnswers(vararg answers: Answer): IBuilder<T>

        /**
         * Устанавливает дополнительные данные произвольного типа
         */
        fun setExtraData(extraData: Any?): IBuilder<T>

        fun build(): T?
    }

    class Builder : IBuilder<Alert> {

        private var title: TextMessage? = null
        private var message: TextMessage? = null
        private var answers: List<Answer> = listOf()
        private var extraData: Any? = null
        private var onClose: (() -> Unit)? = null

        override fun setTitle(title: String?) = setTitle(title?.let(::TextMessage))

        override fun setTitle(@StringRes title: Int?) = setTitle(title?.let(::TextMessage))

        override fun setTitle(title: TextMessage?) = apply {
            this.title = title
        }

        override fun setMessage(message: CharSequence?) = setMessage(message?.let(::TextMessage))

        override fun setMessage(@StringRes message: Int?) = setMessage(message?.let(::TextMessage))

        override fun setMessage(message: TextMessage?) = apply {
            this.message = message
        }

        override fun setAnswers(answers: List<Answer>) = apply {
            this.answers = answers
        }

        override fun setAnswers(vararg answers: Answer) = apply {
            this.answers = answers.toList()
        }

        override fun setExtraData(extraData: Any?) = apply {
            this.extraData = extraData
        }

        fun onClose(onClose: () -> Unit) = apply {
            this.onClose = onClose
        }

        override fun build(): Alert = Alert(
            title,
            message,
            answers,
            extraData,
            onClose
        )
    }

}