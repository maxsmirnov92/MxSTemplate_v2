package net.maxsmr.core.network.retrofit.converters.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.core.network.NO_ERROR_API
import net.maxsmr.core.network.retrofit.converters.BaseEnvelope

@Serializable
internal class VkResponseEnvelope<T>(
    @SerialName("response")
    override val result: T? = null,
    @SerialName("error")
    private val error: Error? = null
): BaseEnvelope<T> {

    override val errorCode: Int = error?.errorCode ?: NO_ERROR_API

    override val errorMessage: String = error?.errorMessage.orEmpty()

    @Serializable
    class Error(
        @SerialName("error_code")
        val errorCode: Int,
        @SerialName("error_msg")
        val errorMessage: String,
    )
}