package net.maxsmr.core.network.retrofit.converters.api

import kotlinx.serialization.Serializable
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.retrofit.converters.BaseResponse

@Serializable
abstract class BaseRadarIoResponse: BaseResponse {

    protected abstract val meta: Meta

    override val errorCode get() = meta.code

    override val errorMessage get() = meta.message

    @Serializable
    data class Meta(
        val code: Int,
        val message: String = EMPTY_STRING,
    )
}