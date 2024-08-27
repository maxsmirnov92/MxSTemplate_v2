package net.maxsmr.core.network.retrofit.converters.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.NO_ERROR_API
import net.maxsmr.core.network.retrofit.converters.BaseEnvelope

@Serializable
internal class YandexGeocodeEnvelope<T>(
    @SerialName("statusCode")
    val statusCode: Int = NO_ERROR_API,
    @SerialName("error")
    val error: String = EMPTY_STRING,
    @SerialName("message")
    val message: String= EMPTY_STRING,
    @SerialName("response")
    val response: T? = null,
): BaseEnvelope<T> {

    override val errorCode: Int = statusCode

    override val errorMessage: String = message

    override val result: T? = response
}