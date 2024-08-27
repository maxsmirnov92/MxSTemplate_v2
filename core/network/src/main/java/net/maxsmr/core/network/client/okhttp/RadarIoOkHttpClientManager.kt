package net.maxsmr.core.network.client.okhttp

import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.network.client.okhttp.interceptors.ApiLoggingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.Authorization
import net.maxsmr.core.network.client.okhttp.interceptors.ConnectivityChecker
import net.maxsmr.core.network.client.okhttp.interceptors.NetworkConnectionInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Invocation
import retrofit2.Retrofit

class RadarIoOkHttpClientManager(
    private val connectivityChecker: ConnectivityChecker,
    private val authorization: String,
    callTimeout: Long,
    retrofitProvider: (() -> Retrofit),
) : BaseOkHttpClientManager(callTimeout, retrofitProvider = retrofitProvider) {

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("RadarIoOkHttpClientManager")

    override fun configureBuild(builder: OkHttpClient.Builder) {
        with(builder) {
            super.configureBuild(this)
            addInterceptor(NetworkConnectionInterceptor(connectivityChecker))
            addInterceptor(RadarIoInterceptor(authorization))
            val loggingInterceptor = ApiLoggingInterceptor { message: String ->
                logger.d(message)
            }
            loggingInterceptor.setLevel(ApiLoggingInterceptor.Level.HEADERS_AND_BODY)
            addInterceptor(loggingInterceptor)
        }
    }

    internal class RadarIoInterceptor(private val authorization: String,) : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()
            val invocation = request.tag(Invocation::class.java)
            if (invocation != null) {
                val needAuthorization = invocation.method().getAnnotation(Authorization::class.java) != null
                if (needAuthorization) {
//                    val subtype = request.body?.contentType()?.subtype
//                    if (subtype == null || subtype.contains("json", true)) {
                    request = request.addHeaderFields(authorization)
//                    }
                }
            }

            return chain.proceed(request)
        }


        private fun Request.addHeaderFields(authorization: String?): Request {
            with(newBuilder()) {
//                addHeader("Content-Type", "application/json")
                authorization?.takeIf { it.isNotEmpty() }?.let {
                    addHeader("Authorization", it)
                }
                return build()
            }
        }
    }
}