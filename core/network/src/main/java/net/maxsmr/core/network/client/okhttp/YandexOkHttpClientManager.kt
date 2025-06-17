package net.maxsmr.core.network.client.okhttp

import net.maxsmr.core.network.appendValues
import net.maxsmr.core.network.client.okhttp.interceptors.ApiLoggingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.Authorization
import net.maxsmr.core.network.client.okhttp.interceptors.BodyCachingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.NetworkConnectionInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Invocation
import java.util.Locale

class YandexOkHttpClientManager(
    private val apiKey: String,
    private val localization: LocalizationField,
    private val defaultLangOrLocale: String = "ru",
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
            addInterceptor(YandexInterceptor())
            super.configureBuild(this)
        }
    }

    enum class LocalizationField {
        LANG, LOCALE
    }

    private inner class YandexInterceptor : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()
            val invocation = request.tag(Invocation::class.java)

            if (invocation != null) {
                request = request.appendValues(appendQueryParametersFunc = {

                    val needAuthorization = invocation.method().getAnnotation(Authorization::class.java) != null
                    if (needAuthorization) {
                        apiKey.takeIf { it.isNotEmpty() }?.let {
                            addQueryParameter("apikey", it)
                        }
                    }

                    addQueryParameter("format", "json")

                    val locale = Locale.getDefault().toString()
                    val lang = when (localization) {
                        LocalizationField.LANG -> {
                            locale.split("_").getOrNull(0)
                        }

                        LocalizationField.LOCALE -> {
                            locale
                        }
                    }
                    addQueryParameter("lang",
                        lang?.takeIf { it.isNotEmpty() } ?: defaultLangOrLocale)
                })
            }

            return chain.proceed(request)
        }
    }
}