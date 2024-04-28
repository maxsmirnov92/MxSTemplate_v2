package net.maxsmr.core.android.base.connection

import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.android.base.alert.representation.AlertRepresentation

/**
 * Обработчик эвентов о смене состояния сети
 *
 * @param onNetworkStateChanged функция реакции на смену состояния сети (например, может быть disable кнопки),
 * либо null, если фрагмент не заинтересован в этом
 * @param alertsMapper функция, определяющая способ отображения алерта при **отсутствии сети**,
 * либо null, если фрагмент не заинтересован в этом
 */
class ConnectionHandler private constructor(
    val onNetworkStateChanged: ((Boolean) -> Unit)? = null,
    val alertsMapper: ((Alert) -> AlertRepresentation?)? = null,
) {

    class Builder {
        private var onNetworkStateChanged: ((Boolean) -> Unit)? = null
        private var alertsMapper: ((Alert) -> AlertRepresentation?)? = null

        /**
         * Задает способ обработки изменения состояния сети.
         *
         * @param handler лямбда, вызываемая с параметром true при появлении соединения,
         * с параметром false - при пропаже
         */
        fun onStateChanged(handler: (Boolean) -> Unit) = apply {
            onNetworkStateChanged = handler
        }

        /**
         * Задает способ преобразования [Alert] в UI-представление [AlertRepresentation].
         *
         * @param handler лямбда, вызываемая при **пропаже** интернет соединения
         */
        fun mapAlerts(handler: (Alert) -> AlertRepresentation?) = apply {
            alertsMapper = handler
        }

        fun build() = ConnectionHandler(onNetworkStateChanged, alertsMapper)
    }
}