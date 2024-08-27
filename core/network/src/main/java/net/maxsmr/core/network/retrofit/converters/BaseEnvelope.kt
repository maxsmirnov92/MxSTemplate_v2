package net.maxsmr.core.network.retrofit.converters

/**
 * Интерфейс-обёртка с [result] конкретного типа
 */
internal interface BaseEnvelope<T>: BaseResponse {

    val result: T?
}