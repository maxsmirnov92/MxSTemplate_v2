package net.maxsmr.core.network.retrofit.client.okhttp

import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.network.SessionStorage
import net.maxsmr.core.network.retrofit.interceptors.ApiLoggingInterceptor
import net.maxsmr.core.network.retrofit.interceptors.Authorization
import net.maxsmr.core.network.retrofit.interceptors.ConnectivityChecker
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Invocation

class RadarIoOkHttpClientManager(
    private val connectivityChecker: ConnectivityChecker,
    private val sessionStorage: SessionStorage,
    callTimeout: Long,
) : BaseOkHttpClientManager(callTimeout) {

    private val logger: BaseLogger = BaseLoggerHolder.instance.getLogger("ApiRequest")

    override fun OkHttpClient.Builder.configureBuild() {
//        addInterceptor(NetworkConnectionInterceptor(connectivityChecker))
//        addInterceptor(RadarIoInterceptor(sessionStorage))
        val loggingInterceptor = ApiLoggingInterceptor { message: String ->
            logger.d(message)
        }
        loggingInterceptor.setLevel(ApiLoggingInterceptor.Level.HEADERS_AND_BODY)
        addInterceptor(loggingInterceptor)
    }

    internal class RadarIoInterceptor(private val sessionStorage: SessionStorage) : Interceptor {

        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()
            val invocation = request.tag(Invocation::class.java)
            if (invocation != null) {
                request.body?.let { requestBody ->
                    val needAuthorization = invocation.method().getAnnotation(Authorization::class.java) != null
                    if (needAuthorization) {
                        val subtype = requestBody.contentType()?.subtype
                        if (subtype == null || subtype.contains("json", true)) {
                            if (subtype == null) {
                                request = request.addHeaderFields(sessionStorage.session)
                            }
                        }
                    }
                }
            }

            return chain.proceed(request)
        }


        private fun Request.addHeaderFields(authorization: String?): Request {
            with(newBuilder()) {
//                addHeader("Content-Type", "application/json")
                authorization?.let {
                    addHeader("Authorization", it)
                }
                return build()
            }
        }
    }


}