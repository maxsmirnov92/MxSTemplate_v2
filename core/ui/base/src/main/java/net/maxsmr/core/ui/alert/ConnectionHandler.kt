package net.maxsmr.core.ui.alert

import net.maxsmr.core.android.base.alert.Alert
import net.maxsmr.core.ui.alert.representation.AlertRepresentation
import net.maxsmr.core.ui.alert.representation.StandardAlertRepresentation

/**
 * Обработчик эвентов о смене состояния сети
 *
 * @param onNetworkStateChanged функция реакции на смену состояния сети (например, может быть disable кнопки),
 * либо null, если фрагмент не заинтересован в этом
 */
class ConnectionHandler(
    val onNetworkStateChanged: ((Boolean) -> Unit)? = null,
)