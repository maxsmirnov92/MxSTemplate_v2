package net.maxsmr.core.network.client.okhttp.interceptors

import net.maxsmr.core.network.client.okhttp.ResponseBodyCache
import net.maxsmr.core.network.client.okhttp.interceptors.annotations.DisableBodyCaching
import net.maxsmr.core.network.okhttp.hasAnnotation
import net.maxsmr.core.network.okhttp.toResponseBody
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody

/**
 * Создаёт копию полученного [ResponseBody]
 * и складывает её в [cache]
 */
class BodyCachingInterceptor(private val cache: ResponseBodyCache<*>) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val disableBodyCaching = request.hasAnnotation<DisableBodyCaching>()
        val response = chain.proceed(request)
        if (!disableBodyCaching || !response.isSuccessful) {
            with(response.toResponseBody(true)) {
                cache.store(request, this)
            }
        }
        return response
    }
}