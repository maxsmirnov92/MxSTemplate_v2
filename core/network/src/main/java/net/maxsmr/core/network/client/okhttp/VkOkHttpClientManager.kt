package net.maxsmr.core.network.client.okhttp

import android.content.Context
import net.maxsmr.core.network.appendValues
import net.maxsmr.core.network.client.okhttp.interceptors.ApiLoggingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.Authorization
import net.maxsmr.core.network.client.okhttp.interceptors.BodyCachingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.ConnectivityChecker
import net.maxsmr.core.network.client.okhttp.interceptors.NetworkConnectionInterceptor
import net.maxsmr.core.network.session.SessionStorage
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Invocation

class VkOkHttpClientManager(
    private val sessionStorage: SessionStorage,
    private val version: String = "5.199",
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
            addInterceptor(VkInterceptor())
            super.configureBuild(this)
        }
    }

    internal inner class VkInterceptor : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()
            val invocation = request.tag(Invocation::class.java)

            if (invocation != null) {
                val needAuthorization = invocation.method().getAnnotation(Authorization::class.java) != null
                if (needAuthorization) {
                    sessionStorage.session?.takeIf { it.isNotEmpty() }?.let { token ->
                        request = request.appendValues(appendQueryParametersFunc = {
                            addQueryParameter("access_token", token)
                            addQueryParameter("v", version)
                        })
                    }
                }
            }

            return chain.proceed(request)
        }
    }
}