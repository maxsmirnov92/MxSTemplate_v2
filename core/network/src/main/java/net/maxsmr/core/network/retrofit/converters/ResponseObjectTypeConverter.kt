package net.maxsmr.core.network.retrofit.converters

import net.maxsmr.core.network.OnServerResponseListener
import net.maxsmr.core.network.ParameterizedTypeImpl
import net.maxsmr.core.network.exceptions.ApiException
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Применяется для случаев, когда производные респонсы дополняют [BaseResponse]
 */
internal class ResponseObjectTypeConverter(
    private val responseListener: OnServerResponseListener? = null,
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *> {
        val objectType =
            (annotations.find { it.annotationClass == ResponseObjectType::class } as ResponseObjectType?)?.value

        if (objectType != null) {
            val envelopedType = objectType.javaObjectType

            val delegate: Converter<ResponseBody, BaseResponse> =
                retrofit.nextResponseBodyConverter(this, envelopedType, annotations)

            return Converter<ResponseBody, Any> { body ->
                val response = delegate.convert(body) as BaseResponse

                responseListener?.onServerResponse(response.errorCode, response.errorMessage, null)

                return@Converter if (response.isOk) {
                    response
                } else {
                    throw ApiException(response.errorCode, response.errorMessage)
                }
            }
        } else {
            throw IllegalStateException("ResponseObjectType annotation is not specified")
        }
    }
}