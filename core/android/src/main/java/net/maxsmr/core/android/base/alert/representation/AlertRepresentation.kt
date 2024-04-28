package net.maxsmr.core.android.base.alert.representation

import net.maxsmr.core.android.base.alert.Alert

/**
 * Абстракция любой формы представления сообщения пользователю
 *
 * @see Alert
 */
interface AlertRepresentation {

    /**
     * Отображает сообщение пользователю
     */
    fun show()

    /**
     * Скрывает сообщение
     */
    fun hide()
}