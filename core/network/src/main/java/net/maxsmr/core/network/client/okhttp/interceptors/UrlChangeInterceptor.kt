package net.maxsmr.core.network.client.okhttp.interceptors

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Подменяет в цепочке url на предоставленный из [urlProvider].
 * Регистрировать первым для корректных логов.
 */
class UrlChangeInterceptor(private val urlProvider: () -> String): Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val url = urlProvider.invoke()
        request = request.newBuilder().url(url).build()
        return chain.proceed(request)
    }
}