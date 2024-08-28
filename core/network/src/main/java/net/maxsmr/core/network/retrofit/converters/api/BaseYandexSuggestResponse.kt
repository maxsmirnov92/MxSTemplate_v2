package net.maxsmr.core.network.retrofit.converters.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.NO_ERROR_API
import net.maxsmr.core.network.retrofit.converters.BaseResponse

@Serializable
open class BaseYandexSuggestResponse: BaseResponse {

    // у Yandex Suggest не приходят внутренние коды/сообщения
    override val errorCode = NO_ERROR_API

    override val errorMessage = EMPTY_STRING

    @SerialName("suggest_reqid")
    val suggestReqId: String = EMPTY_STRING
}