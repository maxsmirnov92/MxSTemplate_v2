package net.maxsmr.core.network.retrofit.converters

import net.maxsmr.core.network.JSON_PARSE_ERROR
import net.maxsmr.core.network.OnServerResponseListener
import net.maxsmr.core.network.ParameterizedTypeImpl
import net.maxsmr.core.network.exceptions.ApiException
import net.maxsmr.core.network.exceptions.NetworkException
import net.maxsmr.core.network.retrofit.internal.cache.OfflineCache
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

internal class EnvelopeObjectTypeConverter<E : BaseEnvelope<*>, O : BaseEnvelopeWithObject<*>>(
    private val envelopeClazz: Class<E>,
    private val envelopeWithObjectClazz: Class<O>? = null,
    private val responseListener: OnServerResponseListener? = null,
) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *> {
        val envelopeObjectType =
            (annotations.find { it.annotationClass == EnvelopeObjectType::class } as EnvelopeObjectType?)?.value

        val offlineCache = annotations.find { it.annotationClass == OfflineCache::class } != null

        if (envelopeObjectType.isNullOrEmpty() || envelopeWithObjectClazz == null) {
            val envelopedType = newParameterizedType(envelopeClazz, type)

            val delegate: Converter<ResponseBody, E> =
                retrofit.nextResponseBodyConverter(this, envelopedType, annotations)

            return Converter<ResponseBody, Any> { body ->
                val envelope = delegate.convert(body) as E

                responseListener?.onServerResponse(envelope.errorCode, envelope.errorMessage/*, envelope.timestamp*/)

//                if (envelope.result is TimestampHolder && envelope.errorCode == 0) {
//                    envelope.timestamp?.let { timestamp ->
//                        envelope.result.timestamp = timestamp
//                    }
//                }

                return@Converter if (envelope.isOk) {
                    envelope.result.takeIf { it != null } ?: throw NetworkException(JSON_PARSE_ERROR, "Envelope result is null")
                } else {
                    throw ApiException(envelope.errorCode, envelope.errorMessage)
                }
            }
        } else {
            val envelopedType =
                if (offlineCache && type is ParameterizedType && type.rawType == Pair::class.javaObjectType) {
                    newParameterizedType(envelopeWithObjectClazz, type.actualTypeArguments[0])
                } else {
                    newParameterizedType(envelopeWithObjectClazz, type)
                }

            val delegate: Converter<ResponseBody, O> =
                retrofit.nextResponseBodyConverter(this, envelopedType, annotations)

            return Converter<ResponseBody, Any> { body ->
                val envelope = delegate.convert(body) as O

                responseListener?.onServerResponse(envelope.errorCode, envelope.errorMessage/*, envelope.timestamp*/)

//                if (envelope.errorCode == 0 && envelope.result?.get(envelopeObjectType) is TimestampHolder) {
//                    envelope.timestamp?.let { timestamp ->
//                        (envelope.result[envelopeObjectType] as TimestampHolder).timestamp = timestamp
//                    }
//                }

                return@Converter if (envelope.isOk) {
                    envelope.result?.get(envelopeObjectType)
                        ?: throw NetworkException(
                            JSON_PARSE_ERROR, "Field \"$envelopeObjectType\" not found in response"
                        )
                } else {
                    throw ApiException(envelope.errorCode, envelope.errorMessage)
                }

            }
        }
    }

    private fun newParameterizedType(rawType: Type, vararg typeArguments: Type): ParameterizedType {
        require(typeArguments.isNotEmpty()) {
            "Missing type arguments for $rawType"
        }
        return ParameterizedTypeImpl(null, rawType, *typeArguments)
    }
}