package net.maxsmr.core.network.client.okhttp

import androidx.annotation.CallSuper
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder
import net.maxsmr.core.network.client.okhttp.interceptors.OkHttpExceptionTransformInterceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

abstract class BaseOkHttpClientManager(
    private val connectTimeout: Long = CONNECT_TIMEOUT_DEFAULT,
    private val readTimeout: Long = connectTimeout,
    private val writeTimeout: Long = connectTimeout,
    private val callTimeout: Long = 0L,
    private val retryOnConnectionFailure: Boolean = RETRY_ON_CONNECTION_FAILURE_DEFAULT,
) {

    @CallSuper
    protected open fun configureBuild(builder: OkHttpClient.Builder) {
        builder.addInterceptor(OkHttpExceptionTransformInterceptor())
    }

    fun build(): OkHttpClient {
        return OkHttpClient.Builder().apply {
            withTimeouts(
                connectTimeout, readTimeout, writeTimeout, callTimeout
            )
            retryOnConnectionFailure(retryOnConnectionFailure)
            configureBuild(this)
        }.build()
    }

    companion object {

        const val CONNECT_TIMEOUT_DEFAULT = 10L
        const val RETRY_ON_CONNECTION_FAILURE_DEFAULT = true

        fun OkHttpClient.Builder.withTimeouts(
            connectTimeout: Long,
            readTimeout: Long = connectTimeout,
            writeTimeout: Long = connectTimeout,
            callTimeout: Long = 0L,
        ) {
            this.connectTimeout(connectTimeout, TimeUnit.SECONDS)
                .readTimeout(readTimeout, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                .callTimeout(callTimeout, TimeUnit.SECONDS)
        }
    }
}