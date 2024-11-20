package net.maxsmr.core.network.api.notification_reader

import kotlinx.serialization.Serializable
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.NO_ERROR_API
import net.maxsmr.core.network.retrofit.converters.BaseResponse

@Serializable
class NotificationReaderDataResponse: BaseResponse {

    override val errorCode = NO_ERROR_API

    override val errorMessage: String = EMPTY_STRING
}