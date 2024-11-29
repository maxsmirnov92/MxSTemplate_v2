package net.maxsmr.core.network.client.okhttp.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * @param timeoutProvider таймаут в секундах
 */
class TimeoutChangeInterceptor(private val timeoutProvider: () -> Long): Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val timeout = timeoutProvider.invoke().takeIf { it >= 0 }?.let {
            TimeUnit.SECONDS.toMillis(it)
        }?.toInt()
        return chain
            .withConnectTimeout(timeout ?: chain.connectTimeoutMillis(), TimeUnit.MILLISECONDS)
            .withReadTimeout(timeout ?: chain.readTimeoutMillis(), TimeUnit.MILLISECONDS)
            .withWriteTimeout(timeout ?: chain.writeTimeoutMillis(), TimeUnit.MILLISECONDS)
            .proceed(request)
    }
}