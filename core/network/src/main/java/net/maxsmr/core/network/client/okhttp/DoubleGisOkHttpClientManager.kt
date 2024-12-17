package net.maxsmr.core.network.client.okhttp

import android.content.Context
import net.maxsmr.core.network.appendValues
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
    private val apiKeyProvider: () -> String,
    connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
    context: Context,
    connectivityChecker: ConnectivityChecker,
    retrofitProvider: () -> Retrofit,
) : BaseRestOkHttpClientManager(
    connectTimeout,
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
            var request = chain.request()
            val invocation = request.tag(Invocation::class.java)

            if (invocation != null) {
                request = request.appendValues(appendQueryParametersFunc = {
                    val needAuthorization = invocation.method().getAnnotation(Authorization::class.java) != null
                    if (needAuthorization) {
                        apiKeyProvider().takeIf { it.isNotEmpty() }?.let { apiKey ->
                            addQueryParameter("key", apiKey)
                        }
                    }
                    addQueryParameter("version", version)
                    addQueryParameter("response_format", "json")
                })
            }

            return chain.proceed(request)
        }
    }
}