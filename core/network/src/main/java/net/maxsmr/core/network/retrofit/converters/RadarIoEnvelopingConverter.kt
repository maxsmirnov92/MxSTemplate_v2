package net.maxsmr.core.network.retrofit.converters

import net.maxsmr.core.network.OnServerResponseListener
import net.maxsmr.core.network.ParameterizedTypeImpl
import net.maxsmr.core.network.exceptions.ApiException
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal class RadarIoEnvelopingConverter(
    private val responseListener: OnServerResponseListener? = null,
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *> {
        val objectType =
            (annotations.find { it.annotationClass == RadarIoResponseObjectType::class } as RadarIoResponseObjectType?)?.value

        if (objectType != null) {
            val envelopedType = objectType.javaObjectType // newParameterizedType(objectType.javaObjectType, type)

            val delegate: Converter<ResponseBody, BaseRadarIoResponse> =
                retrofit.nextResponseBodyConverter(this, envelopedType, annotations)

            return Converter<ResponseBody, Any> { body ->
                val response = delegate.convert(body) as BaseRadarIoResponse

                responseListener?.onServerResponse(response.errorCode, response.errorMessage, null)

                return@Converter if (response.isOk) {
                    response
                } else {
                    throw ApiException(response.errorCode, response.errorMessage)
                }
            }
        } else {
            throw IllegalStateException("RadarIoResponseObjectType annotation is not specified")
        }
    }

    private fun newParameterizedType(rawType: Type, vararg typeArguments: Type): ParameterizedType {
        require(typeArguments.isNotEmpty()) {
            "Missing type arguments for $rawType"
        }
        return ParameterizedTypeImpl(null, rawType, *typeArguments)
    }

}