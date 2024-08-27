package net.maxsmr.core.network.client.okhttp.interceptors

import net.maxsmr.core.network.exceptions.ApiException
import net.maxsmr.core.network.retrofit.converters.BaseResponse
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit

/**
 * Заменяет сообщение в [Response] актуальным из [BaseResponse], если есть
 */
class ResponseErrorMessageInterceptor(
    private val retrofitProvider: () -> Retrofit,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            val baseConverter = retrofitProvider().responseBodyConverter<BaseResponse>(
                BaseResponse::class.java, arrayOfNulls<Annotation>(0)
            )
            if (response.body != null) {
                try {
                    // Читаем тело ответа в буфер, чтобы предотвратить закрытие потока
                    baseConverter.convert(response.peekBody(Long.MAX_VALUE))
                } catch (e: ApiException) {
                    return response.newBuilder().message(
                        e.message?.takeIf { it.isNotEmpty() } ?: response.message
                    ).build()
                }
            }
        }

        return response
    }
}