package net.maxsmr.core.network.retrofit.converters

/**
 * Интерфейс-обёртка с объектами конкретных типов, ассоциированных со своими ключами
 */
internal interface BaseEnvelopeWithObject<T>: BaseResponse {

    val result: Map<String, T?>?
}