package net.maxsmr.core.network.retrofit.converters

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.maxsmr.commonutils.text.EMPTY_STRING
import net.maxsmr.core.network.NO_ERROR
import net.maxsmr.core.network.SUCCESS

@Serializable
abstract class BaseRadarIoResponse {

    protected abstract val meta: Meta

    val errorCode get() = meta.code

    val errorMessage get() = meta.message

    val isOk = errorCode in listOf(SUCCESS, NO_ERROR)

    @Serializable
    data class Meta(
        val code: Int,
        val message: String = EMPTY_STRING,
    )
}