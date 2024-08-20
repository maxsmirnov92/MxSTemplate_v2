package net.maxsmr.core.network.retrofit.client.okhttp

import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.network.SessionStorage
import net.maxsmr.core.network.retrofit.interceptors.ApiLoggingInterceptor
import net.maxsmr.core.network.retrofit.interceptors.Authorization
import net.maxsmr.core.network.retrofit.interceptors.ConnectivityChecker
import net.maxsmr.core.network.retrofit.interceptors.NetworkConnectionInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Invocation

class YandexOkHttpClientManager(
    private val connectivityChecker: ConnectivityChecker,
    private val apiKey: String,
    callTimeout: Long,
) : BaseOkHttpClientManager(callTimeout) {

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("YandexOkHttpClientManager")

    override fun OkHttpClient.Builder.configureBuild() {
        addInterceptor(NetworkConnectionInterceptor(connectivityChecker))
        addInterceptor(YandexInterceptor(apiKey))
        val loggingInterceptor = ApiLoggingInterceptor { message: String ->
            logger.d(message)
        }
        loggingInterceptor.setLevel(ApiLoggingInterceptor.Level.HEADERS_AND_BODY)
        addInterceptor(loggingInterceptor)
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