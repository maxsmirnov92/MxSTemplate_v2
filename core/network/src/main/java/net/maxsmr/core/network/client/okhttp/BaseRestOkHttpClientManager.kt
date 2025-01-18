package net.maxsmr.core.network.client.okhttp

import android.content.Context
import androidx.annotation.CallSuper
import net.maxsmr.core.network.client.okhttp.interceptors.ApiLoggingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.BodyCachingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.ConnectivityChecker
import net.maxsmr.core.network.client.okhttp.interceptors.NetworkConnectionInterceptor
import net.maxsmr.core.network.exceptions.handler.IApiExceptionHandler
import okhttp3.OkHttpClient

abstract class BaseRestOkHttpClientManager(
    connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
    readTimeout: Long = connectTimeout,
    writeTimeout: Long = connectTimeout,
    callTimeout: Long = 0L,
    retryOnConnectionFailure: Boolean = RETRY_ON_CONNECTION_FAILURE_DEFAULT,
    private val context: Context,
    protected val exceptionHandler: IApiExceptionHandler? = null,
    private val connectivityChecker: ConnectivityChecker,
) : BaseOkHttpClientManager(connectTimeout, readTimeout, writeTimeout, callTimeout, retryOnConnectionFailure) {

    @CallSuper
    override fun configureBuild(builder: OkHttpClient.Builder) {
        with(builder) {
            super.configureBuild(this)
            addInterceptor(BodyCachingInterceptor())
            val loggingInterceptor = ApiLoggingInterceptor { message: String ->
                logger.d(message)
            }.apply {
                setLevel(ApiLoggingInterceptor.Level.HEADERS_AND_BODY)
            }
            addInterceptor(loggingInterceptor)
            addInterceptor(NetworkConnectionInterceptor(context, connectivityChecker))
        }
    }
}