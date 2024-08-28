package net.maxsmr.core.network.retrofit.converters.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.NO_ERROR_API
import net.maxsmr.core.network.retrofit.converters.BaseResponse

@Serializable
open class BaseRadarIoResponse: BaseResponse {

    override val errorCode get() = meta.code

    override val errorMessage get() = meta.message

    @SerialName("meta")
    protected val meta: Meta = Meta(NO_ERROR_API)

    @Serializable
    data class Meta(
        val code: Int,
        val message: String = EMPTY_STRING,
    )
}