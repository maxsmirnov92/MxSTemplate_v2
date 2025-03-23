package net.maxsmr.core.network.retrofit.converters.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.NO_ERROR_API
import net.maxsmr.core.network.retrofit.converters.BaseEnvelope

@Serializable
internal class VmOsCloudEnvelope<T>(
    @SerialName("code")
    override val errorCode: Int = NO_ERROR_API,
    @SerialName("msg")
    override val errorMessage: String = EMPTY_STRING,
    @SerialName("data")
    override val result: T? = null,
    val ts: Long = 0
): BaseEnvelope<T>