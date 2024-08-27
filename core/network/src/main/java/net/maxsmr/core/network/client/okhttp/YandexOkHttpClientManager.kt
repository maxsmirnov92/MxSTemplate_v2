package net.maxsmr.core.network.client.okhttp

import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.network.client.okhttp.interceptors.ApiLoggingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.Authorization
import net.maxsmr.core.network.client.okhttp.interceptors.ConnectivityChecker
import net.maxsmr.core.network.client.okhttp.interceptors.NetworkConnectionInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Invocation
import retrofit2.Retrofit

class YandexOkHttpClientManager(
    private val connectivityChecker: ConnectivityChecker,
    private val apiKey: String,
    callTimeout: Long,
    retrofitProvider: (() -> Retrofit),
) : BaseOkHttpClientManager(callTimeout, retrofitProvider = retrofitProvider) {

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("YandexOkHttpClientManager")

    override fun configureBuild(builder: OkHttpClient.Builder) {
        with(builder) {
            super.configureBuild(this)
            addInterceptor(NetworkConnectionInterceptor(connectivityChecker))
            addInterceptor(YandexInterceptor(apiKey))
            val loggingInterceptor = ApiLoggingInterceptor { message: String ->
                logger.d(message)
            }
            loggingInterceptor.setLevel(ApiLoggingInterceptor.Level.HEADERS_AND_BODY)
            addInterceptor(loggingInterceptor)
        }
    }

    internal class YandexInterceptor(private val apiKey: String) : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val invocation = request.tag(Invocation::class.java)

            val url = request.url.newBuilder()
            val newRequest = request.newBuilder()
            url.addQueryParameter("format", "json")
            if (invocation != null) {
                val needAuthorization = invocation.method().getAnnotation(Authorization::class.java) != null
                if (needAuthorization) {
                    apiKey.takeIf { it.isNotEmpty() }?.let {
                        url.addQueryParameter("apikey", it)
                    }
                }
            }

            return chain.proceed(newRequest.url(url.build()).build())
        }
    }
}