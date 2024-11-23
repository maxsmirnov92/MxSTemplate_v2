package net.maxsmr.core.network.client.okhttp

import android.content.Context
import net.maxsmr.core.network.client.okhttp.interceptors.Authorization
import net.maxsmr.core.network.client.okhttp.interceptors.ConnectivityChecker
import net.maxsmr.core.network.retrofit.converters.ResponseObjectType
import net.maxsmr.core.network.retrofit.converters.api.BaseDoubleGisRoutingResponse
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Invocation
import retrofit2.Retrofit

class DoubleGisOkHttpClientManager(
    private val version: String = "2.0",
    private val apiKey: String,
    context: Context,
    connectivityChecker: ConnectivityChecker,
    callTimeout: Long,
    retrofitProvider: () -> Retrofit,
) : BaseRestOkHttpClientManager(
    callTimeout,
    context = context,
    connectivityChecker = connectivityChecker,
    responseAnnotation = ResponseObjectType(BaseDoubleGisRoutingResponse::class),
    retrofitProvider = retrofitProvider
) {

    override fun configureBuild(builder: OkHttpClient.Builder) {
        with(builder) {
            super.configureBuild(this)
            addInterceptor(DoubleGisInterceptor())
        }
    }

    internal inner class DoubleGisInterceptor : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val invocation = request.tag(Invocation::class.java)

            val url = request.url.newBuilder()

            if (invocation != null) {
                val needAuthorization = invocation.method().getAnnotation(Authorization::class.java) != null
                if (needAuthorization) {
                    apiKey.takeIf { it.isNotEmpty() }?.let {
                        url.addQueryParameter("key", it)
                    }
                }
            }
            url.addQueryParameter("version", version)
            url.addQueryParameter("response_format", "json")

            val newRequest = request.newBuilder()
            newRequest.addHeader("Content-Type", "application/json")
            return chain.proceed(newRequest.url(url.build()).build())
        }
    }
}