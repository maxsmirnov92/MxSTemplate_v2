package net.maxsmr.core.network.retrofit.converters

import net.maxsmr.commonutils.ReflectionUtils.invokeMethodOrThrow
import net.maxsmr.core.network.OnServerResponseListener
import net.maxsmr.core.network.exceptions.ApiException
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
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
        val objectTypeAnnotation =
            (annotations.find { it is ResponseObjectType } as? ResponseObjectType)
        val objectType: Class<out BaseResponse>? = objectTypeAnnotation?.getResponseType()

        if (objectType != null) {
            val delegate: Converter<ResponseBody, BaseResponse> =
                retrofit.nextResponseBodyConverter(this, objectType, annotations)

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

    private fun ResponseObjectType.getResponseType(): Class<out BaseResponse>? {
        return try {
            this.value.javaObjectType
        } catch (e: ClassCastException) {
            // при обфускации для javaClass объекта будет сгенерирован свой $ProxyN класс;
            // при простом обращении к value известной kotlin-аннотации ResponseObjectType
            // (изначально это могла быть java-аннотация,
            // но kotlin всё равно будет оборачивать их своими типами)
            // будет исключени "$Proxy7 cannot be cast to n90$b",
            // поскольку прокси-класс не знает о правильном типе,
            // несмотря на все добавленные правила
            val type = this.javaClass
            // поэтому остаётся только вытащить целевой тип под рефлексией из класса ProxyN
            val resultType: Class<out BaseResponse>? = invokeMethodOrThrow<Class<out BaseResponse>>(
                type,
                "value",
                arrayOf(),
                this
            )
            resultType
        }
    }
}