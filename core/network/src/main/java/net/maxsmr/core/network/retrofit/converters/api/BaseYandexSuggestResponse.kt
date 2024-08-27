package net.maxsmr.core.network.retrofit.converters.api

import kotlinx.serialization.Serializable
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.NO_ERROR_API
import net.maxsmr.core.network.retrofit.converters.BaseResponse

@Serializable
abstract class BaseYandexSuggestResponse: BaseResponse {

    abstract val suggestReqId: String

    // у Yandex Suggest не приходят внутренние коды/сообщения
    override val errorCode get() = NO_ERROR_API

    override val errorMessage get() = EMPTY_STRING

}