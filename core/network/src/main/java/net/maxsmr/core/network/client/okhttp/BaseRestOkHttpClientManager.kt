package net.maxsmr.core.network.client.okhttp

import androidx.annotation.CallSuper
import net.maxsmr.core.network.client.okhttp.interceptors.ApiLoggingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.BodyCachingInterceptor
import net.maxsmr.core.network.client.okhttp.interceptors.NetworkConnectionInterceptor
import okhttp3.OkHttpClient

abstract class BaseRestOkHttpClientManager(
    connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
    readTimeout: Long = connectTimeout,
    writeTimeout: Long = connectTimeout,
    callTimeout: Long = 0L,
    retryOnConnectionFailure: Boolean = RETRY_ON_CONNECTION_FAILURE_DEFAULT,
    private val apiLoggingInterceptor: ApiLoggingInterceptor,
    private val cachingInterceptor: BodyCachingInterceptor,
    private val connectionInterceptor: NetworkConnectionInterceptor
) : BaseOkHttpClientManager(connectTimeout, readTimeout, writeTimeout, callTimeout, retryOnConnectionFailure) {

    @CallSuper
    override fun configureBuild(builder: OkHttpClient.Builder) {
        with(builder) {
            super.configureBuild(this)
            addInterceptor(apiLoggingInterceptor)
            addInterceptor(cachingInterceptor)
            addInterceptor(connectionInterceptor)
        }
    }
}