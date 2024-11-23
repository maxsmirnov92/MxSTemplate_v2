package net.maxsmr.core.network.client.okhttp.interceptors

import net.maxsmr.core.network.HostManager
import okhttp3.Interceptor
import okhttp3.Response

class UrlChangeInterceptor(private val hostManagerProvider: () -> HostManager): Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        val manager = hostManagerProvider.invoke()

        request = request.newBuilder().url(
            manager.getBaseUrl()
        ).build()

        return chain.proceed(request)
    }
}