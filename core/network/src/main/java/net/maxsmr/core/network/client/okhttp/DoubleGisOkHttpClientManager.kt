package net.maxsmr.core.network.client.okhttp

import net.maxsmr.core.network.appendValues
import net.maxsmr.core.network.client.okhttp.interceptors.ApiLoggingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.annotations.Authorization
import net.maxsmr.core.network.client.okhttp.interceptors.BodyCachingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.NetworkConnectionInterceptor
import net.maxsmr.core.network.hasAnnotation
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

class DoubleGisOkHttpClientManager(
    private val version: String = "2.0",
    private val apiKeyProvider: () -> String,
    connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
    apiLoggingInterceptor: ApiLoggingInterceptor,
    cachingInterceptor: BodyCachingInterceptor,
    connectionInterceptor: NetworkConnectionInterceptor,
) : BaseRestOkHttpClientManager(
    connectTimeout,
    apiLoggingInterceptor = apiLoggingInterceptor,
    cachingInterceptor = cachingInterceptor,
    connectionInterceptor = connectionInterceptor
) {

    override fun configureBuild(builder: OkHttpClient.Builder) {
        with(builder) {
            addInterceptor(DoubleGisInterceptor())
            super.configureBuild(this)
        }
    }

    private inner class DoubleGisInterceptor : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()
            request = request.appendValues(appendQueryParametersFunc = {
                if (request.hasAnnotation<Authorization>()) {
                    apiKeyProvider().takeIf { it.isNotEmpty() }?.let { apiKey ->
                        addQueryParameter("key", apiKey)
                    }
                }
                addQueryParameter("version", version)
                addQueryParameter("response_format", "json")
            })
            return chain.proceed(request)
        }
    }
}