package net.maxsmr.core.network.client.okhttp

import net.maxsmr.core.network.okhttp.appendValues
import net.maxsmr.core.network.client.okhttp.interceptors.ApiLoggingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.annotations.Authorization
import net.maxsmr.core.network.client.okhttp.interceptors.BodyCachingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.NetworkConnectionInterceptor
import net.maxsmr.core.network.okhttp.hasAnnotation
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.Locale

class RadarIoOkHttpClientManager(
    private val authorization: String,
    private val defaultCountry: String = "RU",
    connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
    apiLoggingInterceptor: ApiLoggingInterceptor,
    cachingInterceptor: BodyCachingInterceptor,
    connectionInterceptor: NetworkConnectionInterceptor
) : BaseRestOkHttpClientManager(
    connectTimeout,
    apiLoggingInterceptor = apiLoggingInterceptor,
    cachingInterceptor = cachingInterceptor,
    connectionInterceptor = connectionInterceptor
) {

    override fun configureBuild(builder: OkHttpClient.Builder) {
        with(builder) {
            addInterceptor(RadarIoInterceptor())
            super.configureBuild(this)
        }
    }

    private inner class RadarIoInterceptor : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()
            request = request.appendValues(
                appendQueryParametersFunc = {
                    val country = Locale.getDefault().toString().split("_")
                        .getOrNull(1)?.takeIf { it.isNotEmpty() } ?: defaultCountry
                    addQueryParameter("country", country)
                },
                appendHeadersFunc = {
                    if (request.hasAnnotation<Authorization>()) {
                        authorization.takeIf { it.isNotEmpty() }?.let {
                            addHeader("Authorization", it)
                        }
                    }
                }
            )
            return chain.proceed(request)
        }
    }
}