package net.maxsmr.core.network.client.okhttp.interceptors

import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.network.exceptions.ApiException
import net.maxsmr.core.network.retrofit.converters.BaseResponse
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit

/**
 * Заменяет сообщение в [Response] актуальным из [BaseResponse], если есть;
 * Применяется при неуспешном ответе,
 * подставляет вручную [responseAnnotation] с целевым классом от [BaseResponse] для парсинга
 */
class ResponseErrorMessageInterceptor(
    private val responseAnnotation: Annotation? = null,
    private val retrofitProvider: () -> Retrofit,
) : Interceptor {

    private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("ResponseErrorMessageInterceptor")

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            val baseConverter = try {
                retrofitProvider().responseBodyConverter<BaseResponse>(
                    BaseResponse::class.java, if (responseAnnotation != null) {
                        arrayOf(responseAnnotation)
                    } else {
                        arrayOfNulls<Annotation>(0)
                    }
                )
            } catch (e: Exception) {
                logger.e(e)
                null
            }
            if (baseConverter != null && response.body != null) {
                try {
                    // Читаем тело ответа в буфер, чтобы предотвратить закрытие потока
                    val baseResponse = baseConverter.convert(response.peekBody(Long.MAX_VALUE))
                    if (baseResponse != null && baseResponse.isOk) {
                        // если в API не подразумеваются внутренние коды
                        throw ApiException(response.code, baseResponse.errorMessage)
                    }
                } catch (e: ApiException) {
                    logger.w(e)
                    e.message?.takeIf { it.isNotEmpty() }?.let {
                        // при наличии errorMessage - продолжаем цепочку с изменённым response
                        return response.newBuilder().message(it).build()
                    }
                }
            }
        }

        return response
    }
}