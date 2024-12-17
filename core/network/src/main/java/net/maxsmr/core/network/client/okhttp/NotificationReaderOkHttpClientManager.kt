package net.maxsmr.core.network.client.okhttp

import android.content.Context
import net.maxsmr.core.network.appendValues
import net.maxsmr.core.network.client.okhttp.interceptors.Authorization
import net.maxsmr.core.network.client.okhttp.interceptors.ConnectivityChecker
import net.maxsmr.core.network.client.okhttp.interceptors.UrlChangeInterceptor
import net.maxsmr.core.network.retrofit.converters.BaseResponse
import net.maxsmr.core.network.retrofit.converters.ResponseObjectType
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Invocation
import retrofit2.Retrofit

class NotificationReaderOkHttpClientManager(
    private val apiKeyProvider: () -> String,
    private val urlProvider: () -> String,
    connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
    retryOnConnectionFailure: Boolean,
    context: Context,
    connectivityChecker: ConnectivityChecker,
    retrofitProvider: () -> Retrofit,
) : BaseRestOkHttpClientManager(
    connectTimeout,
    retryOnConnectionFailure = retryOnConnectionFailure,
    context = context,
    connectivityChecker = connectivityChecker,
    responseAnnotation = ResponseObjectType(BaseResponse::class),
    retrofitProvider = retrofitProvider
) {

    override fun configureBuild(builder: OkHttpClient.Builder) {
        with(builder) {
            addInterceptor(UrlChangeInterceptor(urlProvider))
            super.configureBuild(this)
            addInterceptor(NotificationReaderInterceptor())
        }
    }

    internal inner class NotificationReaderInterceptor : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()
            val invocation = request.tag(Invocation::class.java)

            if (invocation != null) {
                val needAuthorization = invocation.method().getAnnotation(Authorization::class.java) != null
                if (needAuthorization) {
                    apiKeyProvider().takeIf { it.isNotEmpty() }?.let { apiKey ->
                        request = request.appendValues {
                            put("apiKey", apiKey)
                        }
                    }
                }
            }

            return chain.proceed(request)
        }
    }
}