package net.maxsmr.core.network.client.okhttp.interceptors

import net.maxsmr.core.network.client.okhttp.ResponseBodyCache
import net.maxsmr.core.network.toResponseBody
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody

/**
 * Создаёт копию полученного [ResponseBody]
 * и складывает её в [ResponseBodyCache]
 */
class BodyCachingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        ResponseBodyCache.store(request, response.toResponseBody(true))
        return response
    }
}