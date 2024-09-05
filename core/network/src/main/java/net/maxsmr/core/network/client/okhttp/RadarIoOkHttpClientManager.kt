package net.maxsmr.core.network.client.okhttp

import android.content.Context
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
    context: Context,
    connectivityChecker: ConnectivityChecker,
    callTimeout: Long,
    retrofitProvider: (() -> Retrofit),
) : BaseRestOkHttpClientManager(
    callTimeout,
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
            val request = chain.request()
            val newRequest = request.newBuilder()

            val invocation = request.tag(Invocation::class.java)
            if (invocation != null) {
                val needAuthorization = invocation.method().getAnnotation(Authorization::class.java) != null
                if (needAuthorization) {
//                    val subtype = request.body?.contentType()?.subtype
//                    if (subtype == null || subtype.contains("json", true)) {
                    authorization.takeIf { it.isNotEmpty() }?.let {
                        newRequest.addHeader("Authorization", it)
                    }
//                    }
                }
            }

            val country = Locale.getDefault().toString().split("_")
                .getOrNull(1)?.takeIf { it.isNotEmpty() } ?: defaultCountry

            val url = request.url.newBuilder()
            url.addQueryParameter("country", country)

            return chain.proceed(newRequest.url(url.build()).build())
        }
    }
}