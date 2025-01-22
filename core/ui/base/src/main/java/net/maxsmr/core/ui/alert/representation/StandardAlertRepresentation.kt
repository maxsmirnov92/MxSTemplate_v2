package net.maxsmr.core.ui.alert.representation

/**
 * Стандартная форма отображения алерта
 */
interface StandardAlertRepresentation: AlertRepresentation {

    /**
     * Отображает сообщение пользователю
     */
    fun show()

    /**
     * Скрывает сообщение
     */
    fun hide()
}