package net.maxsmr.core.network.client.okhttp

import net.maxsmr.core.network.client.okhttp.interceptors.Authorization
import net.maxsmr.core.network.client.okhttp.interceptors.ConnectivityChecker
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Invocation
import retrofit2.Retrofit
import java.util.Locale

class YandexOkHttpClientManager(
    private val apiKey: String,
    private val localization: LocalizationField,
    private val defaultLangOrLocale: String,
    connectivityChecker: ConnectivityChecker,
    callTimeout: Long,
    responseAnnotation: Annotation?,
    retrofitProvider: (() -> Retrofit),
) : BaseRestOkHttpClientManager(
    callTimeout,
    connectivityChecker = connectivityChecker,
    responseAnnotation = responseAnnotation,
    retrofitProvider = retrofitProvider
) {

    override fun configureBuild(builder: OkHttpClient.Builder) {
        with(builder) {
            super.configureBuild(this)
            addInterceptor(YandexInterceptor())
        }
    }

    enum class LocalizationField {
        LANG, LOCALE
    }

    internal inner class YandexInterceptor : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val invocation = request.tag(Invocation::class.java)

            val url = request.url.newBuilder()
            url.addQueryParameter("format", "json")
            if (invocation != null) {
                val needAuthorization = invocation.method().getAnnotation(Authorization::class.java) != null
                if (needAuthorization) {
                    apiKey.takeIf { it.isNotEmpty() }?.let {
                        url.addQueryParameter("apikey", it)
                    }
                }
            }

            val locale = Locale.getDefault().toString()
            val lang = when(localization) {
                LocalizationField.LANG -> {
                    locale.split("_").getOrNull(0)
                }
                LocalizationField.LOCALE -> {
                    locale
                }
            }
            url.addQueryParameter("lang",
                lang?.takeIf { it.isNotEmpty() } ?: defaultLangOrLocale)

            val newRequest = request.newBuilder()
            return chain.proceed(newRequest.url(url.build()).build())
        }
    }
}