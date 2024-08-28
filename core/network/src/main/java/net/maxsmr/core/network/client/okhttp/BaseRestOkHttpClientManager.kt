package net.maxsmr.core.network.client.okhttp

import androidx.annotation.CallSuper
import net.maxsmr.core.network.client.okhttp.interceptors.ApiLoggingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.ConnectivityChecker
import net.maxsmr.core.network.client.okhttp.interceptors.NetworkConnectionInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.ResponseErrorMessageInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit

abstract class BaseRestOkHttpClientManager(
    callTimeout: Long,
    readTimeout: Long = 0L,
    writeTimeout: Long = 0L,
    connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
    retryOnConnectionFailure: Boolean = RETRY_ON_CONNECTION_FAILURE_DEFAULT,
    private val connectivityChecker: ConnectivityChecker,
    private val responseAnnotation: Annotation? = null,
    private val retrofitProvider: (() -> Retrofit)? = null,
): BaseOkHttpClientManager(callTimeout, readTimeout, writeTimeout, connectTimeout, retryOnConnectionFailure) {

    @CallSuper
    override fun configureBuild(builder: OkHttpClient.Builder) {
        with(builder) {
            val loggingInterceptor = ApiLoggingInterceptor { message: String ->
                logger.d(message)
            }.apply {
                setLevel(ApiLoggingInterceptor.Level.HEADERS_AND_BODY)
            }
            addInterceptor(loggingInterceptor)
            addInterceptor(NetworkConnectionInterceptor(connectivityChecker))
            retrofitProvider?.let {
                addInterceptor(ResponseErrorMessageInterceptor(responseAnnotation, it))
            }
        }
    }
}