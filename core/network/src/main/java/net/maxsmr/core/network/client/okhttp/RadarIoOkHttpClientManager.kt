package net.maxsmr.core.network.client.okhttp

import android.content.Context
import net.maxsmr.core.network.appendValues
import net.maxsmr.core.network.client.okhttp.interceptors.Authorization
import net.maxsmr.core.network.client.okhttp.interceptors.ConnectivityChecker
import net.maxsmr.core.network.retrofit.converters.ResponseObjectType
import net.maxsmr.core.network.retrofit.converters.api.BaseRadarIoResponse
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Invocation
import retrofit2.Retrofit
import java.util.Locale

class RadarIoOkHttpClientManager(
    private val authorization: String,
    private val defaultCountry: String = "RU",
    connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
    context: Context,
    connectivityChecker: ConnectivityChecker,
    retrofitProvider: (() -> Retrofit),
) : BaseRestOkHttpClientManager(
    connectTimeout,
    context = context,
    connectivityChecker = connectivityChecker,
    responseAnnotation = ResponseObjectType(BaseRadarIoResponse::class),
    retrofitProvider = retrofitProvider
) {

    override fun configureBuild(builder: OkHttpClient.Builder) {
        with(builder) {
            super.configureBuild(this)
            addInterceptor(RadarIoInterceptor())
        }
    }

    internal inner class RadarIoInterceptor : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()
            val invocation = request.tag(Invocation::class.java)

            if (invocation != null) {
                request = request.appendValues(
                    appendQueryParametersFunc = {
                        val country = Locale.getDefault().toString().split("_")
                            .getOrNull(1)?.takeIf { it.isNotEmpty() } ?: defaultCountry
                        addQueryParameter("country", country)
                    },
                    appendHeadersFunc = {
                        val needAuthorization = invocation.method().getAnnotation(Authorization::class.java) != null
                        if (needAuthorization) {
                            authorization.takeIf { it.isNotEmpty() }?.let {
                                addHeader("Authorization", it)
                            }
                        }
                    }
                )
            }

            return chain.proceed(request)
        }
    }
}