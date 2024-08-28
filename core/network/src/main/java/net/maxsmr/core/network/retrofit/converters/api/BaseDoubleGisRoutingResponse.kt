package net.maxsmr.core.network.retrofit.converters.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.NO_ERROR_API
import net.maxsmr.core.network.retrofit.converters.BaseResponse

@Serializable
open class BaseDoubleGisRoutingResponse(): BaseResponse {

    // нет внутреннего кода
    override val errorCode = NO_ERROR_API

    @SerialName("message")
    override val errorMessage: String = EMPTY_STRING

    // при успехе время генерации ответа
    @SerialName("generation_time")
    val generationTime: Long = 0

    @SerialName("type")
    val type: String = EMPTY_STRING
}