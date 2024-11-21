package net.maxsmr.core.network.client.okhttp

import android.content.Context
import net.maxsmr.core.network.appendValues
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
    private val defaultLangOrLocale: String = "ru",
    connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
    context: Context,
    connectivityChecker: ConnectivityChecker,
    responseAnnotation: Annotation?,
    retrofitProvider: () -> Retrofit,
) : BaseRestOkHttpClientManager(
    connectTimeout,
    context = context,
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