package net.maxsmr.core.network.retrofit.converters.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.NO_ERROR_API
import net.maxsmr.core.network.retrofit.converters.BaseEnvelope

@Serializable
internal class YandexGeocodeEnvelope<T>(
    @SerialName("statusCode")
    override val errorCode: Int = NO_ERROR_API,
    @SerialName("error")
    val error: String = EMPTY_STRING,
    @SerialName("message")
    override val errorMessage: String = EMPTY_STRING,
    @SerialName("response")
    override val result: T? = null,
) : BaseEnvelope<T>