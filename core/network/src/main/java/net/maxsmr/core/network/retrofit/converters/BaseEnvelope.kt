package net.maxsmr.core.network.retrofit.converters

/**
 * Интерфейс-обёртка с [result] конкретного типа
 */
internal interface BaseEnvelope<T> {

    val errorCode: Int

    val errorMessage: String

    val result: T?
}