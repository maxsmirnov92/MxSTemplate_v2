package net.maxsmr.core.network.client.okhttp.interceptors

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.network.exceptions.ApiException
import net.maxsmr.core.network.exceptions.handler.CallExceptionHandler
import net.maxsmr.core.network.retrofit.converters.BaseResponse
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit

/**
 * При неуспешном http-ответе парсит тело для оповещения [ApiException] в [handler],
 * подставляет вручную [responseAnnotation] с целевым классом от [BaseResponse] для парсинга;
 * Заменяет сообщение в [Response] внутренним, если есть
 */
@Deprecated("use ExceptionHandlingCallAdapterFactory")
class HttpResponseErrorInterceptor(
    private val handler: CallExceptionHandler? = null,
    private val responseAnnotation: Annotation? = null,
    private val retrofitProvider: () -> Retrofit,
) : Interceptor {

    private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>("HttpResponseErrorInterceptor")

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (!response.isSuccessful && response.body != null) {
            // неуспешный на уровне http респонс, но есть тело, внутренний ответ которого сл-но надо учесть
            val baseConverter = retrofitProvider().responseBodyConverter<BaseResponse>(
                BaseResponse::class.java, if (responseAnnotation != null) {
                    arrayOf(responseAnnotation)
                } else {
                    arrayOfNulls<Annotation>(0)
                }
            )
            try {
                // Читаем тело ответа в буфер, чтобы предотвратить закрытие потока
                baseConverter.convert(response.peekBody(Long.MAX_VALUE))
            } catch (e: Exception) {
                // convert может выкинуть что-то отличное от ApiException
                // (например, если в теле не json),
                if (e is ApiException) {
                    logger.w(e)
                    handler?.let {
                        scope.launch {
                            it.onException(e)
                        }
                    }
                    // проброс API exception не сработает,
                    // будет исходный retrofit2.HttpException,
                    // поэтому остаётся просто подменить сообщение в респонсе на внутреннее
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